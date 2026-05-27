package com.pengyouquan.english.service;

import com.pengyouquan.english.model.User;
import com.pengyouquan.english.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("金币服务测试")
class GoldServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoldService goldService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setGold(100);
    }

    // ── 查询余额 ──

    @Test
    @DisplayName("查询余额：用户存在返回当前金币数")
    void getBalance_userExists_returnsGold() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        int balance = goldService.getBalance(1L);

        assertThat(balance).isEqualTo(100);
    }

    @Test
    @DisplayName("查询余额：用户不存在返回0")
    void getBalance_userNotExists_returnsZero() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        int balance = goldService.getBalance(999L);

        assertThat(balance).isEqualTo(0);
    }

    @Test
    @DisplayName("查询余额：gold为null时返回0")
    void getBalance_goldIsNull_returnsZero() {
        testUser.setGold(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        int balance = goldService.getBalance(1L);

        assertThat(balance).isEqualTo(0);
    }

    // ── 增加金币 ──

    @Test
    @DisplayName("增加金币：余额正确增加并返回新余额")
    void addGold_increasesBalance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        int newBalance = goldService.addGold(1L, 50);

        assertThat(newBalance).isEqualTo(150);
        assertThat(testUser.getGold()).isEqualTo(150);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("增加金币：连续增加两次余额累加")
    void addGold_multipleAdditions_accumulates() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        goldService.addGold(1L, 30);
        goldService.addGold(1L, 20);

        assertThat(testUser.getGold()).isEqualTo(150);
    }

    @Test
    @DisplayName("增加金币：用户不存在返回0")
    void addGold_userNotExists_returnsZero() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        int result = goldService.addGold(999L, 50);

        assertThat(result).isEqualTo(0);
        verify(userRepository, never()).save(any());
    }

    // ── 消费金币 ──

    @Test
    @DisplayName("消费金币：余额充足扣除成功返回true")
    void deductGold_sufficientBalance_returnsTrue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = goldService.deductGold(1L, 30);

        assertThat(result).isTrue();
        assertThat(testUser.getGold()).isEqualTo(70);
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("消费金币：余额不足返回false且不变更余额")
    void deductGold_insufficientBalance_returnsFalse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        boolean result = goldService.deductGold(1L, 200);

        assertThat(result).isFalse();
        assertThat(testUser.getGold()).isEqualTo(100);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("消费金币：用户不存在返回false")
    void deductGold_userNotExists_returnsFalse() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        boolean result = goldService.deductGold(999L, 10);

        assertThat(result).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("消费金币：余额恰好相等时扣除成功")
    void deductGold_exactBalance_returnsTrue() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        boolean result = goldService.deductGold(1L, 100);

        assertThat(result).isTrue();
        assertThat(testUser.getGold()).isEqualTo(0);
    }

    // ── 每日首胜 ──

    @Test
    @DisplayName("每日首胜增加20金币")
    void addDailyWinBonus_adds20Gold() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        int result = goldService.addDailyWinBonus(1L);

        assertThat(result).isEqualTo(120);
        assertThat(testUser.getGold()).isEqualTo(120);
    }
}
