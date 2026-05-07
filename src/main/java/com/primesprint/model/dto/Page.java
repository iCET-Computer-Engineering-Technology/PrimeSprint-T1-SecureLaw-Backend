package com.primesprint.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.function.Function;

@Setter
@Getter
public class Page<T> {
    private List<T> content;
    private int totalPages;
    private long totalElements;
    private int size;
    private int number;

    public Page() {
    }

    public Page(List<T> content, int totalPages, long totalElements, int size, int number) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.size = size;
        this.number = number;
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedContent = content == null ? null : content.stream().map(mapper).collect(java.util.stream.Collectors.toList());
        return new Page<>(mappedContent, totalPages, totalElements, size, number);
    }
}
