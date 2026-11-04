package com.podo.domain.cs.service;

import com.podo.domain.cs.dto.request.NoticeCreateRequest;
import com.podo.domain.cs.dto.request.NoticeUpdateRequest;
import com.podo.domain.cs.dto.response.NoticeListResponse;
import com.podo.domain.cs.dto.response.NoticeResponse;
import com.podo.domain.cs.entity.Notice;
import com.podo.domain.cs.exception.NoticeNotFoundException;
import com.podo.domain.cs.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public Page<NoticeListResponse> findAll(String keyword, Pageable pageable) {
        String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;
        return noticeRepository.search(trimmedKeyword, pageable)
                .map(NoticeListResponse::from);
    }

    // Todo 중복 조회 방지
    @Transactional
    public NoticeResponse findById(Long id) {
        Notice notice = findNoticeById(id);
        notice.increaseViewCount();
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse create(NoticeCreateRequest request) {
        Notice notice = noticeRepository.save(Notice.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .pinned(request.isPinned())
                .build());

        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeUpdateRequest request) {
        Notice notice = findNoticeById(id);
        notice.update(request.getTitle(), request.getContent(), request.isPinned());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(Long id) {
        Notice notice = findNoticeById(id);
        noticeRepository.delete(notice);
    }

    public NoticeResponse findByIdForAdmin(Long id) {
        return NoticeResponse.from(findNoticeById(id));
    }

    private Notice findNoticeById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NoticeNotFoundException(id));
    }
}
