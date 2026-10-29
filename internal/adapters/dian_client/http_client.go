package dian_client

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"house-expenses/internal/core/ports"
)

var (
	ErrDianServiceUnavailable = errors.New("dian microservice is currently unreachable")
	ErrDianServiceTimeout     = errors.New("dian microservice query timed out")
	ErrDianNitRequired        = errors.New("la DIAN requiere el NIT del emisor o del receptor para validar este documento")
)

type HTTPDianClient struct {
	baseURL    string
	httpClient *http.Client
}

func NewHTTPDianClient(baseURL string) *HTTPDianClient {
	if baseURL == "" {
		baseURL = "http://localhost:8081"
	}
	return &HTTPDianClient{
		baseURL: strings.TrimRight(baseURL, "/"),
		httpClient: &http.Client{
			Timeout: 20 * time.Second,
		},
	}
}

type lookupRequest struct {
	QRURL string `json:"qr_url,omitempty"`
	CUFE  string `json:"cufe,omitempty"`
	NIT   string `json:"nit,omitempty"`
}

func (c *HTTPDianClient) LookupInvoice(ctx context.Context, qrURLOrCUFE string, nit string) (*ports.DianInvoiceDTO, error) {
	reqBody := lookupRequest{}
	if strings.HasPrefix(qrURLOrCUFE, "http://") || strings.HasPrefix(qrURLOrCUFE, "https://") {
		reqBody.QRURL = qrURLOrCUFE
	} else {
		reqBody.CUFE = qrURLOrCUFE
	}
	if nit != "" {
		reqBody.NIT = nit
	}

	payload, err := json.Marshal(reqBody)
	if err != nil {
		return nil, err
	}

	endpoint := fmt.Sprintf("%s/api/v1/dian/lookup", c.baseURL)
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, endpoint, bytes.NewBuffer(payload))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		if errors.Is(ctx.Err(), context.DeadlineExceeded) {
			return nil, ErrDianServiceTimeout
		}
		return nil, fmt.Errorf("%w: %v", ErrDianServiceUnavailable, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnprocessableEntity {
		return nil, ErrDianNitRequired
	}

	if resp.StatusCode != http.StatusOK {
		respBytes, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("dian service returned error %d: %s", resp.StatusCode, string(respBytes))
	}

	var dto ports.DianInvoiceDTO
	if err := json.NewDecoder(resp.Body).Decode(&dto); err != nil {
		return nil, fmt.Errorf("failed to decode dian service response: %w", err)
	}

	return &dto, nil
}

func (c *HTTPDianClient) DownloadPDF(ctx context.Context, cufe string, nit string) (io.ReadCloser, error) {
	endpoint := fmt.Sprintf("%s/api/v1/dian/pdf/%s", c.baseURL, cufe)
	if nit != "" {
		endpoint = fmt.Sprintf("%s?nit=%s", endpoint, nit)
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return nil, err
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		if errors.Is(ctx.Err(), context.DeadlineExceeded) {
			return nil, ErrDianServiceTimeout
		}
		return nil, fmt.Errorf("%w: %v", ErrDianServiceUnavailable, err)
	}

	if resp.StatusCode != http.StatusOK {
		defer resp.Body.Close()
		respBytes, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("dian pdf download failed with status %d: %s", resp.StatusCode, string(respBytes))
	}

	return resp.Body, nil
}
