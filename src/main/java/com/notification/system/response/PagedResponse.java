package com.notification.system.response;

import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * Explicit pagination shape returned to clients, instead of serializing
 * Spring Data's {@code Page<T>} directly — keeps the API contract decoupled
 * from Spring Data's internal representation.
 */
@Getter
public class PagedResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean last;

    public PagedResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
    }
}
