package com.codesense.repository.service;

import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RepositoryProcessingLock {

    private final ConcurrentMap<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock forRepository(UUID repositoryId) {
        return locks.computeIfAbsent(repositoryId, ignored -> new ReentrantLock());
    }
}