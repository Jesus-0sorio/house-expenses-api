package oauth

import (
	"fmt"
	"sync"

	"house-expenses/internal/core/domain"
	"house-expenses/internal/core/ports"
)

type ProviderRegistry struct {
	mu        sync.RWMutex
	providers map[domain.AuthProvider]ports.OAuthProvider
}

func NewProviderRegistry() *ProviderRegistry {
	return &ProviderRegistry{
		providers: make(map[domain.AuthProvider]ports.OAuthProvider),
	}
}

func (r *ProviderRegistry) Register(provider ports.OAuthProvider) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.providers[provider.GetProviderName()] = provider
}

func (r *ProviderRegistry) Get(name domain.AuthProvider) (ports.OAuthProvider, error) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	provider, exists := r.providers[name]
	if !exists {
		return nil, fmt.Errorf("oauth provider '%s' is not registered or configured", name)
	}
	return provider, nil
}
