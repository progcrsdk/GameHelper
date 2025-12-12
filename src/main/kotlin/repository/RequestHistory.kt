package org.example.repository

import org.example.entity.RequestHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RequestHistoryRepository : JpaRepository<RequestHistory, Long>