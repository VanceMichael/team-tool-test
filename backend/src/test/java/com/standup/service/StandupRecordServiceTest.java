package com.standup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.standup.common.BizException;
import com.standup.context.UserContext;
import com.standup.dto.StandupRecordVO;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.UserMapper;
import com.standup.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StandupRecordServiceTest {

    private StandupRecordService standupRecordService;

    @Mock
    private StandupRecordMapper standupRecordMapper;

    @Mock
    private UserMapper userMapper;

    private MockedStatic<UserContext> userContextMock;

    @BeforeEach
    void setUp() throws Exception {
        userContextMock = mockStatic(UserContext.class);

        Class<?> implClass = Class.forName("com.standup.service.impl.StandupRecordServiceImpl");
        Object impl = implClass.getDeclaredConstructor().newInstance();

        Field baseMapperField = ServiceImpl.class.getDeclaredField("baseMapper");
        baseMapperField.setAccessible(true);
        baseMapperField.set(impl, standupRecordMapper);

        Field userMapperField = implClass.getDeclaredField("userMapper");
        userMapperField.setAccessible(true);
        userMapperField.set(impl, userMapper);

        standupRecordService = (StandupRecordService) impl;
    }

    @AfterEach
    void tearDown() {
        userContextMock.close();
    }

    private void mockUserContext(Long userId, String role) {
        userContextMock.when(UserContext::getUserId).thenReturn(userId);
        userContextMock.when(UserContext::getRole).thenReturn(role);
    }

    @Nested
    @DisplayName("submit 方法测试")
    class SubmitTests {

        @Test
        @DisplayName("提交站会_正常提交_应成功返回记录")
        void 正常提交_应成功返回记录() {
            Long userId = 1L;
            Long teamId = 1L;
            mockUserContext(userId, "MEMBER");

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);
            request.setYesterday("昨天完成了A");
            request.setToday("今天计划做B");
            request.setBlocker("无");

            when(standupRecordMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
            when(standupRecordMapper.insert(any(StandupRecord.class))).thenReturn(1);

            StandupRecord result = standupRecordService.submit(request);

            assertNotNull(result);
            assertEquals(userId, result.getUserId());
            assertEquals(teamId, result.getTeamId());
            assertEquals(request.getYesterday(), result.getYesterday());
            assertEquals(request.getToday(), result.getToday());
            assertEquals(request.getBlocker(), result.getBlocker());
            verify(standupRecordMapper).insert(any(StandupRecord.class));
        }

        @Test
        @DisplayName("提交站会_今天已提交过_应抛出异常")
        void 今天已提交过_应抛出异常() {
            Long userId = 1L;
            Long teamId = 1L;
            mockUserContext(userId, "MEMBER");

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);
            request.setYesterday("昨天完成了A");
            request.setToday("今天计划做B");

            StandupRecord existing = TestDataFactory.createRecord(1L, userId, teamId, LocalDate.now());
            when(standupRecordMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

            BizException ex = assertThrows(BizException.class, () -> standupRecordService.submit(request));
            assertEquals("今天已经提交过站会记录", ex.getMessage());
            verify(standupRecordMapper, never()).insert(any(StandupRecord.class));
        }
    }

    @Nested
    @DisplayName("updateRecord 方法测试")
    class UpdateRecordTests {

        @Test
        @DisplayName("更新站会记录_本人1小时内编辑_应成功")
        void 本人1小时内编辑_应成功() {
            Long userId = 1L;
            Long teamId = 1L;
            Long recordId = 1L;
            mockUserContext(userId, "MEMBER");

            StandupRecord record = TestDataFactory.createRecord(recordId, userId, teamId, LocalDate.now());
            record.setCreateTime(LocalDateTime.now().minusMinutes(30));

            when(standupRecordMapper.selectById(recordId)).thenReturn(record);
            when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);
            request.setYesterday("更新后的昨天");
            request.setToday("更新后的今天");
            request.setBlocker("新阻塞");

            StandupRecord result = standupRecordService.updateRecord(recordId, request);

            assertEquals("更新后的昨天", result.getYesterday());
            assertEquals("更新后的今天", result.getToday());
            assertEquals("新阻塞", result.getBlocker());
            verify(standupRecordMapper).updateById(record);
        }

        @Test
        @DisplayName("更新站会记录_本人超过1小时编辑_应抛出异常")
        void 本人超过1小时编辑_应抛出异常() {
            Long userId = 1L;
            Long teamId = 1L;
            Long recordId = 1L;
            mockUserContext(userId, "MEMBER");

            StandupRecord record = TestDataFactory.createRecord(recordId, userId, teamId, LocalDate.now());
            record.setCreateTime(LocalDateTime.now().minusHours(2));

            when(standupRecordMapper.selectById(recordId)).thenReturn(record);

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);

            BizException ex = assertThrows(BizException.class, () -> standupRecordService.updateRecord(recordId, request));
            assertEquals("提交超过1小时，不能再编辑", ex.getMessage());
            verify(standupRecordMapper, never()).updateById(any(StandupRecord.class));
        }

        @Test
        @DisplayName("更新站会记录_非本人非管理员_应抛出异常")
        void 非本人非管理员_应抛出异常() {
            Long userId = 1L;
            Long otherUserId = 2L;
            Long teamId = 1L;
            Long recordId = 1L;
            mockUserContext(otherUserId, "MEMBER");

            StandupRecord record = TestDataFactory.createRecord(recordId, userId, teamId, LocalDate.now());
            record.setCreateTime(LocalDateTime.now().minusMinutes(30));

            when(standupRecordMapper.selectById(recordId)).thenReturn(record);

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);

            BizException ex = assertThrows(BizException.class, () -> standupRecordService.updateRecord(recordId, request));
            assertEquals("只能编辑自己的站会记录", ex.getMessage());
        }

        @Test
        @DisplayName("更新站会记录_管理员编辑他人记录_应成功不受时间限制")
        void 管理员编辑他人记录_应成功不受时间限制() {
            Long ownerId = 1L;
            Long adminId = 2L;
            Long teamId = 1L;
            Long recordId = 1L;
            mockUserContext(adminId, "ADMIN");

            StandupRecord record = TestDataFactory.createRecord(recordId, ownerId, teamId, LocalDate.now());
            record.setCreateTime(LocalDateTime.now().minusHours(5));

            when(standupRecordMapper.selectById(recordId)).thenReturn(record);
            when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(teamId);
            request.setYesterday("管理员更新");

            StandupRecord result = standupRecordService.updateRecord(recordId, request);

            assertEquals("管理员更新", result.getYesterday());
            verify(standupRecordMapper).updateById(record);
        }

        @Test
        @DisplayName("更新站会记录_记录不存在_应抛出异常")
        void 记录不存在_应抛出异常() {
            Long userId = 1L;
            Long recordId = 999L;
            mockUserContext(userId, "MEMBER");

            when(standupRecordMapper.selectById(recordId)).thenReturn(null);

            StandupSubmitRequest request = new StandupSubmitRequest();
            request.setTeamId(1L);

            BizException ex = assertThrows(BizException.class, () -> standupRecordService.updateRecord(recordId, request));
            assertEquals("站会记录不存在", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("getTeamRecords 方法测试")
    class GetTeamRecordsTests {

        @Test
        @DisplayName("获取团队记录_返回记录列表_应正确转换为VO")
        void 返回记录列表_应正确转换为VO() {
            Long userId = 1L;
            Long teamId = 1L;
            mockUserContext(userId, "MEMBER");

            LocalDate today = LocalDate.now();
            StandupRecord record = TestDataFactory.createRecord(1L, userId, teamId, today);
            record.setCreateTime(LocalDateTime.now().minusMinutes(30));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
            User user = TestDataFactory.createMemberUser(userId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertEquals(1, result.size());
            assertEquals(userId, result.get(0).getUserId());
            assertEquals(teamId, result.get(0).getTeamId());
            assertEquals(user.getNickname(), result.get(0).getNickname());
        }

        @Test
        @DisplayName("获取团队记录_空列表_应返回空列表")
        void 空列表_应返回空列表() {
            Long teamId = 1L;
            mockUserContext(1L, "MEMBER");

            LocalDate today = LocalDate.now();
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("convertToVO editable 字段测试")
    class ConvertToVOEditableTests {

        @Test
        @DisplayName("editable判定_管理员_应始终为true")
        void 管理员_应始终为true() {
            Long ownerId = 1L;
            Long teamId = 1L;
            mockUserContext(99L, "ADMIN");

            LocalDate today = LocalDate.now();
            StandupRecord record = TestDataFactory.createRecord(1L, ownerId, teamId, today);
            record.setCreateTime(LocalDateTime.now().minusHours(3));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
            User user = TestDataFactory.createMemberUser(ownerId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertTrue(result.get(0).isEditable());
        }

        @Test
        @DisplayName("editable判定_本人1小时内_应为true")
        void 本人1小时内_应为true() {
            Long userId = 1L;
            Long teamId = 1L;
            mockUserContext(userId, "MEMBER");

            LocalDate today = LocalDate.now();
            StandupRecord record = TestDataFactory.createRecord(1L, userId, teamId, today);
            record.setCreateTime(LocalDateTime.now().minusMinutes(30));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
            User user = TestDataFactory.createMemberUser(userId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertTrue(result.get(0).isEditable());
        }

        @Test
        @DisplayName("editable判定_本人超时_应为false")
        void 本人超时_应为false() {
            Long userId = 1L;
            Long teamId = 1L;
            mockUserContext(userId, "MEMBER");

            LocalDate today = LocalDate.now();
            StandupRecord record = TestDataFactory.createRecord(1L, userId, teamId, today);
            record.setCreateTime(LocalDateTime.now().minusHours(3));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
            User user = TestDataFactory.createMemberUser(userId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertFalse(result.get(0).isEditable());
        }

        @Test
        @DisplayName("editable判定_其他人_应为false")
        void 其他人_应为false() {
            Long ownerId = 1L;
            Long otherUserId = 2L;
            Long teamId = 1L;
            mockUserContext(otherUserId, "MEMBER");

            LocalDate today = LocalDate.now();
            StandupRecord record = TestDataFactory.createRecord(1L, ownerId, teamId, today);
            record.setCreateTime(LocalDateTime.now().minusMinutes(30));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
            User user = TestDataFactory.createMemberUser(ownerId);
            when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(teamId, today);

            assertFalse(result.get(0).isEditable());
        }
    }
}
