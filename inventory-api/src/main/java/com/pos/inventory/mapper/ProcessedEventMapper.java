package com.pos.inventory.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProcessedEventMapper {

    /** @return 1 when the event is new, 0 when it was already processed. */
    @Insert("INSERT INTO processed_events (event_id) VALUES (#{eventId}) ON CONFLICT DO NOTHING")
    int markProcessed(@Param("eventId") String eventId);
}
