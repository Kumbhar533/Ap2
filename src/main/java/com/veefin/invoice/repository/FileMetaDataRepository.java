package com.veefin.invoice.repository;

import com.veefin.invoice.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileMetaDataRepository  extends JpaRepository<FileMetadata, Long> {
}
