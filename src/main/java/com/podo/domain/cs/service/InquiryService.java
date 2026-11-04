package com.podo.domain.cs.service;

import com.podo.domain.cs.dto.request.InquiryAnswerRequest;
import com.podo.domain.cs.dto.request.InquiryCreateRequest;
import com.podo.domain.cs.dto.request.InquiryUpdateRequest;
import com.podo.domain.cs.dto.response.AdminInquiryListResponse;
import com.podo.domain.cs.dto.response.AdminInquiryResponse;
import com.podo.domain.cs.dto.response.InquiryListResponse;
import com.podo.domain.cs.dto.response.InquiryResponse;
import com.podo.domain.cs.entity.Inquiry;
import com.podo.domain.cs.entity.InquiryStatus;
import com.podo.domain.cs.exception.InquiryAlreadyAnsweredException;
import com.podo.domain.cs.exception.InquiryNotFoundException;
import com.podo.domain.cs.exception.NotInquiryWriterException;
import com.podo.domain.cs.repository.InquiryRepository;
import com.podo.domain.user.entity.User;
import com.podo.domain.user.exception.UserNotFoundException;
import com.podo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;

    @Transactional
    public InquiryResponse create(Long userId, InquiryCreateRequest request) {
        User user = findUserById(userId);

        Inquiry inquiry = inquiryRepository.save(Inquiry.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .build());

        return InquiryResponse.from(inquiry);
    }

    public Page<InquiryListResponse> findMyInquiries(Long userId, String keyword, Pageable pageable) {
        User user = findUserById(userId);
        String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return inquiryRepository.findByUser(user, trimmedKeyword, pageable)
                .map(InquiryListResponse::from);
    }

    public InquiryResponse findById(Long id, Long userId) {
        Inquiry inquiry = findInquiryById(id);
        User user = findUserById(userId);
        validateWriter(inquiry, user);
        return InquiryResponse.from(inquiry);
    }

    @Transactional
    public InquiryResponse update(Long id, Long userId, InquiryUpdateRequest request) {
        Inquiry inquiry = findInquiryById(id);
        User user = findUserById(userId);
        validateWriter(inquiry, user);

        if (inquiry.isAnswered()) {
            throw new InquiryAlreadyAnsweredException();
        }

        inquiry.updateContent(request.getTitle(), request.getContent());
        return InquiryResponse.from(inquiry);
    }

    public Page<AdminInquiryListResponse> findAllForAdmin(InquiryStatus status, String keyword, Pageable pageable) {
        String trimmedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return inquiryRepository.searchForAdmin(status, keyword, pageable)
                .map(AdminInquiryListResponse::from);
    }

    public AdminInquiryResponse findByIdForAdmin(Long id) {
        return AdminInquiryResponse.from(findInquiryById(id));
    }

    @Transactional
    public AdminInquiryResponse answer(Long id, InquiryAnswerRequest request) {
        Inquiry inquiry = findInquiryById(id);
        inquiry.registerAnswer(request.getAnswer());
        return AdminInquiryResponse.from(inquiry);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Inquiry findInquiryById(Long id) {
        return inquiryRepository.findById(id)
                .orElseThrow(() -> new InquiryNotFoundException(id));
    }

    private void validateWriter(Inquiry inquiry, User user) {
        if (!inquiry.isWrittenBy(user)) {
            throw new NotInquiryWriterException();
        }
    }
}
