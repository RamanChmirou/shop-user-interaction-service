package com.kanapa4.shop.userinteraction.repository;

import com.kanapa4.shop.userinteraction.model.CheckoutDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CheckoutDraftRepository extends JpaRepository<CheckoutDraft, UUID> {
}
