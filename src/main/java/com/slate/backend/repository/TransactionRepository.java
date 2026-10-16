package com.slate.backend.repository;

import com.slate.backend.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByClientIdOrderByCreatedAtDesc(Long clientId);

    List<Transaction> findByClientUserIdOrderByCreatedAtDesc(Long userId);
}
