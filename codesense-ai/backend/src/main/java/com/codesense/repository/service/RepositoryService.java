    @Transactional
    public RepositoryDto updateRepository(String email, UUID repositoryId, UpdateRepositoryRequest request) {
        Repository repo = getRepositoryEntityWithOwnerCheck(email, repositoryId);
        
        // Update name and description
        repo.setName(request.getName());
        if (request.getDescription() != null) {
            repo.setDescription(request.getDescription());
        }
        
        repo = repositoryRepo.save(repo);
        log.info("Updated repository {} with new metadata", repositoryId);
        
        return toDto(repo);
    }
