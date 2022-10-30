package com.upgrade.volcanocampsitereservation.repository;

import com.upgrade.volcanocampsitereservation.dto.BookingDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;

@Repository
public interface BookingRepository extends JpaRepository<BookingDto, Long> {
  // Version field is automatically incremented
  @Lock(LockModeType.PESSIMISTIC_FORCE_INCREMENT)
  <S extends BookingDto> S save(S entity);
}
