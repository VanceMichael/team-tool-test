package com.standup.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.standup.common.BizException;
import com.standup.context.UserContext;
import com.standup.dto.StandupRecordVO;
import com.standup.dto.StandupSubmitRequest;
import com.standup.entity.StandupRecord;
import com.standup.entity.User;
import com.standup.mapper.StandupRecordMapper;
import com.standup.mapper.UserMapper;
import com.standup.service.impl.StandupRecordServiceImpl;
import com.standup.testutil.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StandupRecordServiceImpl 单元测试")
class StandupRecordServiceImplTest {

    @Mock
    private StandupRecordMapper standupRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    @org.mockito.Spy
    private StandupRecordServiceImpl standupRecordService;

    private static final Long USER_ID = 1L;
    private static final Long TEAM_ID = 1L;
    private static final Long RECORD_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(standupRecordService, "baseMapper", standupRecordMapper);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("提交站会_正常提交_应成功")
    void 提交站会_正常提交_应成功() {
        UserContext.set(USER_ID, "MEMBER");
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);

        doReturn(null).when(standupRecordService).getOne(any(LambdaQueryWrapper.class));
        when(standupRecordMapper.insert(any(StandupRecord.class))).thenAnswer(invocation -> {
            StandupRecord record = invocation.getArgument(0);
            record.setId(RECORD_ID);
            return 1;
        });

        StandupRecord result = standupRecordService.submit(request);

        assertNotNull(result);
        assertEquals(USER_ID, result.getUserId());
        assertEquals(TEAM_ID, result.getTeamId());
        assertEquals(LocalDate.now(), result.getRecordDate());
        verify(standupRecordMapper).insert(any(StandupRecord.class));
    }

    @Test
    @DisplayName("提交站会_今天已提交过_应抛出异常")
    void 提交站会_今天已提交过_应抛出异常() {
        UserContext.set(USER_ID, "MEMBER");
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);
        StandupRecord existing = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now());

        doReturn(existing).when(standupRecordService).getOne(any(LambdaQueryWrapper.class));

        BizException exception = assertThrows(BizException.class, () -> standupRecordService.submit(request));
        assertEquals("今天已经提交过站会记录", exception.getMessage());
        verify(standupRecordMapper, never()).insert(any(StandupRecord.class));
    }

    @Test
    @DisplayName("更新记录_本人1小时内_应成功")
    void 更新记录_本人1小时内_应成功() {
        UserContext.set(USER_ID, "MEMBER");
        LocalDateTime createTime = LocalDateTime.now().minusMinutes(30);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now(), createTime);
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);
        request.setYesterday("更新后的昨天内容");

        when(standupRecordMapper.selectById(RECORD_ID)).thenReturn(record);
        when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

        StandupRecord result = standupRecordService.updateRecord(RECORD_ID, request);

        assertEquals("更新后的昨天内容", result.getYesterday());
        verify(standupRecordMapper).updateById(any(StandupRecord.class));
    }

    @Test
    @DisplayName("更新记录_本人超过1小时_应抛出异常")
    void 更新记录_本人超过1小时_应抛出异常() {
        UserContext.set(USER_ID, "MEMBER");
        LocalDateTime createTime = LocalDateTime.now().minusHours(2);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now(), createTime);
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);

        when(standupRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        BizException exception = assertThrows(BizException.class, () -> standupRecordService.updateRecord(RECORD_ID, request));
        assertEquals("提交超过1小时，不能再编辑", exception.getMessage());
        verify(standupRecordMapper, never()).updateById(any(StandupRecord.class));
    }

    @Test
    @DisplayName("更新记录_非本人非管理员_应抛出异常")
    void 更新记录_非本人非管理员_应抛出异常() {
        UserContext.set(2L, "MEMBER");
        LocalDateTime createTime = LocalDateTime.now();
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now(), createTime);
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);

        when(standupRecordMapper.selectById(RECORD_ID)).thenReturn(record);

        BizException exception = assertThrows(BizException.class, () -> standupRecordService.updateRecord(RECORD_ID, request));
        assertEquals("只能编辑自己的站会记录", exception.getMessage());
        verify(standupRecordMapper, never()).updateById(any(StandupRecord.class));
    }

    @Test
    @DisplayName("更新记录_管理员编辑超时记录_应成功")
    void 更新记录_管理员编辑超时记录_应成功() {
        UserContext.set(99L, "ADMIN");
        LocalDateTime createTime = LocalDateTime.now().minusHours(5);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now(), createTime);
        StandupSubmitRequest request = TestDataFactory.createSubmitRequest(TEAM_ID);
        request.setToday("管理员更新的内容");

        when(standupRecordMapper.selectById(RECORD_ID)).thenReturn(record);
        when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

        StandupRecord result = standupRecordService.updateRecord(RECORD_ID, request);

        assertEquals("管理员更新的内容", result.getToday());
        verify(standupRecordMapper).updateById(any(StandupRecord.class));
    }

    @Test
    @DisplayName("获取团队记录_指定日期_应返回列表")
    void 获取团队记录_指定日期_应返回列表() {
        LocalDate date = LocalDate.now();
        StandupRecord record1 = TestDataFactory.createRecord(1L, 1L, TEAM_ID, date);
        StandupRecord record2 = TestDataFactory.createRecord(2L, 2L, TEAM_ID, date);
        User user1 = TestDataFactory.createMemberUser(1L);
        User user2 = TestDataFactory.createMemberUser(2L);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record1, record2));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user1, user2));

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertEquals(2, result.size());
        assertEquals("用户1", result.get(0).getNickname());
        assertEquals("用户2", result.get(1).getNickname());
    }

    @Test
    @DisplayName("获取团队记录_无记录_应返回空列表")
    void 获取团队记录_无记录_应返回空列表() {
        LocalDate date = LocalDate.now();

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertTrue(result.isEmpty());
        verify(userMapper, never()).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("转换VO_管理员查看_editable应为true")
    void 转换VO_管理员查看_editable应为true() {
        UserContext.set(99L, "ADMIN");
        LocalDate date = LocalDate.now();
        LocalDateTime createTime = LocalDateTime.now().minusHours(5);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, 2L, TEAM_ID, date, createTime);
        User user = TestDataFactory.createMemberUser(2L);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isEditable());
    }

    @Test
    @DisplayName("转换VO_本人1小时内_editable应为true")
    void 转换VO_本人1小时内_editable应为true() {
        UserContext.set(USER_ID, "MEMBER");
        LocalDate date = LocalDate.now();
        LocalDateTime createTime = LocalDateTime.now().minusMinutes(30);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, date, createTime);
        User user = TestDataFactory.createMemberUser(USER_ID);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isEditable());
    }

    @Test
    @DisplayName("转换VO_本人超时_editable应为false")
    void 转换VO_本人超时_editable应为false() {
        UserContext.set(USER_ID, "MEMBER");
        LocalDate date = LocalDate.now();
        LocalDateTime createTime = LocalDateTime.now().minusHours(2);
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, date, createTime);
        User user = TestDataFactory.createMemberUser(USER_ID);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isEditable());
    }

    @Test
    @DisplayName("转换VO_其他人记录_editable应为false")
    void 转换VO_其他人记录_editable应为false() {
        UserContext.set(USER_ID, "MEMBER");
        LocalDate date = LocalDate.now();
        LocalDateTime createTime = LocalDateTime.now();
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, 2L, TEAM_ID, date, createTime);
        User user = TestDataFactory.createMemberUser(2L);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

        List<StandupRecordVO> result = standupRecordService.getTeamRecords(TEAM_ID, date);

        assertEquals(1, result.size());
        assertFalse(result.get(0).isEditable());
    }

    @Test
    @DisplayName("获取团队日期范围内记录_应返回正确结果")
    void 获取团队日期范围内记录_应返回正确结果() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();
        StandupRecord record = TestDataFactory.createRecord(RECORD_ID, USER_ID, TEAM_ID, LocalDate.now());
        User user = TestDataFactory.createMemberUser(USER_ID);

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(userMapper.selectBatchIds(anyList())).thenReturn(List.of(user));

        List<StandupRecordVO> result = standupRecordService.getTeamRecordsInRange(TEAM_ID, startDate, endDate);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取团队日期范围内记录_空范围_应返回空列表")
    void 获取团队日期范围内记录_空范围_应返回空列表() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        List<StandupRecordVO> result = standupRecordService.getTeamRecordsInRange(TEAM_ID, startDate, endDate);

        assertTrue(result.isEmpty());
    }
}
