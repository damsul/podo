package com.podo.global.init;

import com.podo.domain.cs.entity.Faq;
import com.podo.domain.cs.entity.FaqCategory;
import com.podo.domain.cs.entity.Inquiry;
import com.podo.domain.cs.entity.Notice;
import com.podo.domain.payroom.entity.*;
import com.podo.domain.user.entity.Role;
import com.podo.domain.user.entity.SocialType;
import com.podo.domain.user.entity.User;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final EntityManager em;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🍇 Podo 더미데이터 생성 시작!");

        // 1. 사용자 생성
        List<User> users = createUsers();

        // 2. 더치페이 방 + 멤버 생성
        PayRoom mtRoom = createMtRoom(users);
        PayRoom netflixRoom = createNetflixRoom(users);

        // 3. 결제 항목 생성
        createMtPayments(mtRoom, users);
        createNetflixPayments(netflixRoom, users);

        // 4. 계좌 정보 생성
        createAccounts(mtRoom, users);

        // 4-1. 권한 확인용: 외부인의 방 (다른 사용자와 겹치지 않음)
        createOutsiderRoom(users);

        // 5. 고객센터 데이터 생성
        createNotices();
        createFaqs();
        createInquiries(users);


        log.info("✅ Podo 더미데이터 생성 완료!");
    }

    private List<User> createUsers() {
        User admin = User.builder()
                .loginId("admin")
                .password(passwordEncoder.encode("password123"))
                .name("관리자")
                .email("admin@podo.com")
                .role(Role.ADMIN)
                .build();

        User gyounghwan = User.builder()
                .loginId("pien")
                .password(passwordEncoder.encode("password123"))
                .name("최경환")
                .email("pien@naver.com")
                .role(Role.USER)
                .build();

        User minwook = User.builder()
                .loginId("minwook")
                .password(passwordEncoder.encode("password123"))
                .name("이민욱")
                .email("minwook@daum.net")
                .role(Role.USER)
                .build();

        User juhwang = User.builder()
                .loginId("orange")
                .name("박주황")
                .email("orange@gmail.com")
                .socialType(SocialType.GOOGLE)
                .role(Role.USER)
                .build();

        User youngjun = User.builder()
                .loginId("gotchan")
                .password(passwordEncoder.encode("password123"))
                .name("윤영찬")
                .email("gotchan@gmail.com")
                .role(Role.USER)
                .build();

        User sohyun = User.builder()
                .loginId("sosoo")
                .name("진소현")
                .socialType(SocialType.NAVER)
                .role(Role.USER)
                .build();

        User outsider = User.builder()
                .loginId("outsider")
                .password(passwordEncoder.encode("password123"))
                .name("김외부")
                .email("outsider@podo.com")
                .role(Role.USER)
                .build();

        em.persist(admin);
        em.persist(gyounghwan);
        em.persist(minwook);
        em.persist(juhwang);
        em.persist(youngjun);
        em.persist(sohyun);
        em.persist(outsider);

        log.info("👤 사용자 7명 생성 완료");
        return List.of(admin, gyounghwan, minwook, juhwang, youngjun, sohyun, outsider);
    }

    private PayRoom createMtRoom(List<User> users) {
        User gyounghwan = users.get(1);
        User minwook = users.get(2);
        User juhwang = users.get(3);
        User youngjun = users.get(4);
        User sohyun = users.get(5);

        PayRoom mtRoom = PayRoom.builder()
                .owner(gyounghwan)
                .title("Podo MT")
                .dueDate(LocalDate.of(2026, 6, 12))
                .roundUnit(10)
                .build();
        em.persist(mtRoom);

        for (User user : List.of(gyounghwan, minwook, juhwang, youngjun, sohyun)) {
            em.persist(RoomMember.builder()
                    .payRoom(mtRoom)
                    .user(user)
                    .build());
        }

        log.info("🏠 Podo MT 방 생성 (참여자 5명)");
        return mtRoom;
    }

    private PayRoom createNetflixRoom(List<User> users) {
        User minwook = users.get(2);
        User gyounghwan = users.get(1);
        User sohyun = users.get(5);

        PayRoom netflixRoom = PayRoom.builder()
                .owner(minwook)
                .title("넷플릭스 정기결제")
                .roundUnit(10)
                .build();
        em.persist(netflixRoom);

        for (User user : List.of(minwook, gyounghwan, sohyun)) {
            em.persist(RoomMember.builder()
                    .payRoom(netflixRoom)
                    .user(user)
                    .build());
        }
        netflixRoom.complete();

        log.info("🎬 넷플릭스 정기결제 방 생성 (정산 완료)");
        return netflixRoom;
    }
    private void createMtPayments(PayRoom mtRoom, List<User> users) {
        User gyounghwan = users.get(1);
        User minwook = users.get(2);
        User juhwang = users.get(3);
        User youngjun = users.get(4);
        User sohyun = users.get(5);

        // 결제 1: 장보기 - 경환이 80,000원 (5명 모두 나눔)
        Payment p1 = Payment.builder()
                .payRoom(mtRoom)
                .payer(gyounghwan)
                .title("장보기")
                .amount(80000)
                .paidAt(LocalDate.of(2026, 5, 1))
                .build();
        em.persist(p1);

        for (User user : List.of(gyounghwan, minwook, juhwang, youngjun, sohyun)) {
            PaymentParticipant pp = PaymentParticipant.builder()
                    .payment(p1)
                    .user(user)
                    .build();

            // 경환(자기 결제)는 정산 완료, 민욱은 송금 완료
            if (user.equals(gyounghwan) || user.equals(minwook)) {
                pp.markAsSettled();
            }
            em.persist(pp);
        }

        // 결제 2: 야식 배달 - 소현이 20,000원 (3명만 나눔)
        Payment p2 = Payment.builder()
                .payRoom(mtRoom)
                .payer(sohyun)
                .title("야식 배달")
                .amount(20000)
                .paidAt(LocalDate.of(2026, 5, 1))
                .build();
        em.persist(p2);

        for (User user : List.of(gyounghwan, youngjun, sohyun)) {
            em.persist(PaymentParticipant.builder()
                    .payment(p2)
                    .user(user)
                    .build());
        }

        // 쿼리 수 측정용: 결제 8건 추가 (총 10건)
        for (int i = 1; i <= 8; i++) {
            Payment extra = Payment.builder()
                    .payRoom(mtRoom)
                    .payer(i % 2 == 0 ? gyounghwan : minwook)
                    .title("측정용 결제 " + i)
                    .amount(10000 * i)
                    .paidAt(LocalDate.of(2026, 5, 2))
                    .build();
            em.persist(extra);

            for (User user : List.of(gyounghwan, minwook, juhwang, youngjun, sohyun)) {
                em.persist(PaymentParticipant.builder()
                        .payment(extra)
                        .user(user)
                        .build());
            }
        }

        log.info("💰 Podo MT 결제 2건 생성");
    }

    private void createNetflixPayments(PayRoom netflixRoom, List<User> users) {
        User minwook = users.get(2);
        User gyounghwan = users.get(1);
        User sohyun = users.get(5);

        Payment p1 = Payment.builder()
                .payRoom(netflixRoom)
                .payer(minwook)
                .title("5월 구독료")
                .amount(17000)
                .paidAt(LocalDate.of(2026, 5, 5))
                .build();
        em.persist(p1);

        for (User user : List.of(minwook, gyounghwan, sohyun)) {
            PaymentParticipant pp = PaymentParticipant.builder()
                    .payment(p1)
                    .user(user)
                    .build();
            pp.markAsSettled();
            em.persist(pp);
        }

        log.info("🎬 넷플릭스 결제 1건 생성 (모두 정산 완료)");
    }

    private void createAccounts(PayRoom mtRoom, List<User> users) {
        User gyounghwan = users.get(1);
        User minwook = users.get(2);

        em.persist(Account.builder()
                .payRoom(mtRoom)
                .user(gyounghwan)
                .bank("카카오뱅크")
                .accountNumber("3333-09-2693847")
                .holderName("최경환")
                .build());

        em.persist(Account.builder()
                .payRoom(mtRoom)
                .user(minwook)
                .bank("신한은행")
                .accountNumber("110-363-290257")
                .holderName("이민욱")
                .build());

        log.info("🏦 Podo MT 계좌 2개 등록");
    }

    private void createOutsiderRoom(List<User> users) {
        User outsider = users.get(6);

        PayRoom room = PayRoom.builder()
                .owner(outsider)
                .title("외부인 방")
                .roundUnit(10)
                .build();
        em.persist(room);

        em.persist(RoomMember.builder()
                .payRoom(room)
                .user(outsider)
                .build());

        Payment payment = Payment.builder()
                .payRoom(room)
                .payer(outsider)
                .title("혼밥")
                .amount(12000)
                .paidAt(LocalDate.of(2026, 5, 10))
                .build();
        em.persist(payment);

        em.persist(PaymentParticipant.builder()
                .payment(payment)
                .user(outsider)
                .build());

        em.persist(Account.builder()
                .payRoom(room)
                .user(outsider)
                .bank("국민은행")
                .accountNumber("000-00-0000000")
                .holderName("김외부")
                .build());

        log.info("🚪 권한 확인용 외부인 방 생성");
    }

    private void createNotices() {
        em.persist(Notice.builder()
                .title("[중요] Podo 서비스 정식 오픈 안내")
                .content("안녕하세요! 더치페이의 모든 것, Podo가 정식 오픈했습니다 🍇\n\n친구들과의 더치페이를 더 쉽고 깔끔하게...")
                .pinned(true)
                .build());

        em.persist(Notice.builder()
                .title("5월 정기 점검 안내")
                .content("매월 첫째 주 일요일 새벽 2시~4시 정기 점검이 진행됩니다.")
                .build());

        em.persist(Notice.builder()
                .title("개인정보 처리방침 개정 안내")
                .content("2026년 5월 1일부터 개인정보 처리방침이 일부 개정됩니다...")
                .build());

        log.info("📢 공지사항 3개 생성");
    }

    private void createFaqs() {
        em.persist(Faq.builder()
                .category(FaqCategory.ACCOUNT)
                .question("회원가입은 어떻게 하나요?")
                .answer("로그인 화면에서 '회원가입' 버튼을 클릭하시면 됩니다. 소셜 로그인(카카오/네이버/구글)도 지원합니다.")
                .displayOrder(0)
                .build());

        em.persist(Faq.builder()
                .category(FaqCategory.PAYROOM)
                .question("페이방은 몇 명까지 참여 가능한가요?")
                .answer("페이방당 최대 50명까지 참여할 수 있습니다.")
                .displayOrder(0)
                .build());

        em.persist(Faq.builder()
                .category(FaqCategory.PAYROOM)
                .question("방장이 누구인지 변경할 수 있나요?")
                .answer("현재는 방장 변경 기능을 지원하지 않습니다. 추후 업데이트 예정입니다.")
                .displayOrder(1)
                .build());

        em.persist(Faq.builder()
                .category(FaqCategory.PAYMENT)
                .question("정산 금액은 어떻게 계산되나요?")
                .answer("결제별로 1/n 계산 후, 절사 옵션(1원/10원/100원/1000원)에 따라 절사됩니다.")
                .displayOrder(0)
                .build());

        em.persist(Faq.builder()
                .category(FaqCategory.ETC)
                .question("앱이 갑자기 느려져요")
                .answer("브라우저 캐시 삭제 후 다시 시도해주세요. 그래도 해결되지 않으면 1:1 문의를 남겨주세요.")
                .displayOrder(0)
                .build());

        log.info("❓ FAQ 5개 생성");
    }

    private void createInquiries(List<User> users) {
        User gyounghwan = users.get(1);
        User juhwang = users.get(3);

        // 답변 대기 중인 문의
        em.persist(Inquiry.builder()
                .user(juhwang)
                .title("정산 계산이 이상한 것 같아요")
                .content("장보기 80,000원을 5명이 나누면 16,000원씩인데 왜 다르게 보이나요?")
                .build());

        // 답변 완료된 문의
        Inquiry answered = Inquiry.builder()
                .user(gyounghwan)
                .title("계좌번호 변경 가능한가요?")
                .content("등록한 계좌번호를 수정하고 싶어요!")
                .build();
        answered.registerAnswer("네, 페이방 상세 화면에서 계좌 옆 연필 아이콘을 클릭하시면 수정 가능합니다.");
        em.persist(answered);

        log.info("📩 1:1 문의 2개 생성 (답변완료 1건, 대기중 1건)");
    }
}
