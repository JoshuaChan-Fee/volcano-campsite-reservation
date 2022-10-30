package com.upgrade.volcanocampsitereservation.repository;

import com.upgrade.volcanocampsitereservation.dto.BookingDateDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.util.stream.Stream;

@Repository
public interface BookingDateRepository extends JpaRepository<BookingDateDto, LocalDate> {

  String FIND_DATES_BETWEEN_QUERY = "select d from #{#entityName} d where d.date >= ?1 and d.date < ?2";

  // saving without conflict use case
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(FIND_DATES_BETWEEN_QUERY)
  Stream<BookingDateDto> findAllDatesBetween(LocalDate startInclusive, LocalDate endExclusive);

  // This will search will be faster
  @Query(FIND_DATES_BETWEEN_QUERY)
  Stream<BookingDateDto> quickFindAllDatesBetween(LocalDate startInclusive, LocalDate endExclusive);
}
