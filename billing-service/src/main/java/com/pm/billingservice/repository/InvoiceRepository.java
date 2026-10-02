package com.pm.billingservice.repository;

import com.pm.billingservice.model.Invoice;
import com.pm.billingservice.model.InvoiceStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository
    extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

  @Override
  @EntityGraph(attributePaths = "account")
  Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

  @EntityGraph(attributePaths = "account")
  Optional<Invoice> findWithAccountById(UUID id);

  @EntityGraph(attributePaths = "account")
  List<Invoice> findByAccountIdAndStatus(UUID accountId, InvoiceStatus status);

  /** Rows of [status, sum(amount), count] for one account. */
  @Query("select i.status, sum(i.amount), count(i) from Invoice i "
      + "where i.account.id = :accountId group by i.status")
  List<Object[]> summarize(@Param("accountId") UUID accountId);

  @Query(value = "select nextval('invoice_number_seq')", nativeQuery = true)
  long nextInvoiceSequence();
}
