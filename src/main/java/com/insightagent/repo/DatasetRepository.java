package com.insightagent.repo;

import com.insightagent.domain.AppUser;
import com.insightagent.domain.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    List<Dataset> findByOwnerOrderByUploadedAtDesc(AppUser owner);

    Optional<Dataset> findByIdAndOwner(Long id, AppUser owner);
}
