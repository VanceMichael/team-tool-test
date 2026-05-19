package com.standup.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("站会记录服务测试")
class StandupRecordServiceImplTest {

    @Mock
    private StandupRecordMapper standupRecordMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StandupRecordServiceImpl standupRecordService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(standupRecordService, "baseMapper", standupRecordMapper);
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, StandupRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Nested
    @DisplayName("提交站会记录")
    class SubmitTest {

        @Test
        @DisplayName("正常提交成功")
        void 提交站会_正常情况_应提交成功() {
            UserContext.set(1L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            when(standupRecordMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(null);
            when(standupRecordMapper.insert(any(StandupRecord.class))).thenReturn(1);

            StandupRecord result = standupRecordService.submit(request);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getTeamId()).isEqualTo(100L);
            assertThat(result.getRecordDate()).isEqualTo(LocalDate.now());
            assertThat(result.getYesterday()).isEqualTo(request.getYesterday());
            verify(standupRecordMapper).insert(any(StandupRecord.class));
        }

        @Test
        @DisplayName("同一用户同一天同一团队重复提交抛出异常")
        void 提交站会_今天已提交过_应抛出异常() {
            UserContext.set(1L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            StandupRecord existing = TestDataFactory.createRecord(1L, 1L, 100L, LocalDate.now());
            when(standupRecordMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(existing);

            assertThatThrownBy(() -> standupRecordService.submit(request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("今天已经提交过站会记录");

            verify(standupRecordMapper, never()).insert(any(StandupRecord.class));
        }
    }

    @Nested
    @DisplayName("更新站会记录")
    class UpdateRecordTest {

        @Test
        @DisplayName("本人在1小时内可编辑")
        void 更新记录_本人一小时内_应编辑成功() {
            UserContext.set(1L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            StandupRecord record = TestDataFactory.createRecord(
                    1L, 1L, 100L, LocalDate.now(), LocalDateTime.now().minusMinutes(30)
            );
            when(standupRecordMapper.selectById(1L)).thenReturn(record);
            when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

            StandupRecord result = standupRecordService.updateRecord(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getYesterday()).isEqualTo(request.getYesterday());
            verify(standupRecordMapper).updateById(any(StandupRecord.class));
        }

        @Test
        @DisplayName("本人超过1小时不可编辑抛出异常")
        void 更新记录_本人超过一小时_应抛出异常() {
            UserContext.set(1L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            StandupRecord record = TestDataFactory.createRecord(
                    1L, 1L, 100L, LocalDate.now(), LocalDateTime.now().minusHours(2)
            );
            when(standupRecordMapper.selectById(1L)).thenReturn(record);

            assertThatThrownBy(() -> standupRecordService.updateRecord(1L, request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("提交超过1小时，不能再编辑");

            verify(standupRecordMapper, never()).updateById(any(StandupRecord.class));
        }

        @Test
        @DisplayName("非本人非管理员不可编辑")
        void 更新记录_非本人非管理员_应抛出异常() {
            UserContext.set(2L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            StandupRecord record = TestDataFactory.createRecord(
                    1L, 1L, 100L, LocalDate.now(), LocalDateTime.now().minusMinutes(30)
            );
            when(standupRecordMapper.selectById(1L)).thenReturn(record);

            assertThatThrownBy(() -> standupRecordService.updateRecord(1L, request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("只能编辑自己的站会记录");

            verify(standupRecordMapper, never()).updateById(any(StandupRecord.class));
        }

        @Test
        @DisplayName("管理员可以编辑任何人的记录不受时间限制")
        void 更新记录_管理员编辑他人记录_应成功() {
            UserContext.set(99L, "ADMIN");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            StandupRecord record = TestDataFactory.createRecord(
                    1L, 1L, 100L, LocalDate.now(), LocalDateTime.now().minusHours(5)
            );
            when(standupRecordMapper.selectById(1L)).thenReturn(record);
            when(standupRecordMapper.updateById(any(StandupRecord.class))).thenReturn(1);

            StandupRecord result = standupRecordService.updateRecord(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getYesterday()).isEqualTo(request.getYesterday());
            verify(standupRecordMapper).updateById(any(StandupRecord.class));
        }

        @Test
        @DisplayName("记录不存在抛出异常")
        void 更新记录_记录不存在_应抛出异常() {
            UserContext.set(1L, "MEMBER");
            StandupSubmitRequest request = TestDataFactory.createSubmitRequest(100L);

            when(standupRecordMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> standupRecordService.updateRecord(999L, request))
                    .isInstanceOf(BizException.class)
                    .hasMessage("站会记录不存在");
        }
    }

    @Nested
    @DisplayName("获取团队站会记录")
    class GetTeamRecordsTest {

        @Test
        @DisplayName("返回指定日期的记录列表")
        void 获取团队记录_有记录_应返回列表() {
            UserContext.set(1L, "MEMBER");
            LocalDate date = LocalDate.now();

            StandupRecord record1 = TestDataFactory.createRecord(1L, 1L, 100L, date, LocalDateTime.now().minusMinutes(10));
            StandupRecord record2 = TestDataFactory.createRecord(2L, 2L, 100L, date, LocalDateTime.now().minusMinutes(20));
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record1, record2));

            User user1 = TestDataFactory.createMember(1L, "成员一");
            User user2 = TestDataFactory.createMember(2L, "成员二");
            when(userMapper.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(user1, user2));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNickname()).isEqualTo("成员一");
            assertThat(result.get(1).getNickname()).isEqualTo("成员二");
        }

        @Test
        @DisplayName("无记录时返回空列表")
        void 获取团队记录_无记录_应返回空列表() {
            UserContext.set(1L, "MEMBER");
            LocalDate date = LocalDate.now();

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("convertToVO中editable字段判定")
    class ConvertToVOEditableTest {

        @Test
        @DisplayName("管理员始终可编辑")
        void editable判定_管理员_应为true() {
            UserContext.set(99L, "ADMIN");
            LocalDate date = LocalDate.now();

            StandupRecord record = TestDataFactory.createRecord(1L, 1L, 100L, date, LocalDateTime.now().minusHours(5));
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            User user = TestDataFactory.createMember(1L, "成员");
            when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result.get(0).isEditable()).isTrue();
        }

        @Test
        @DisplayName("本人1小时内可编辑")
        void editable判定_本人一小时内_应为true() {
            UserContext.set(1L, "MEMBER");
            LocalDate date = LocalDate.now();

            StandupRecord record = TestDataFactory.createRecord(1L, 1L, 100L, date, LocalDateTime.now().minusMinutes(30));
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            User user = TestDataFactory.createMember(1L, "成员");
            when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result.get(0).isEditable()).isTrue();
        }

        @Test
        @DisplayName("本人超过1小时不可编辑")
        void editable判定_本人超过一小时_应为false() {
            UserContext.set(1L, "MEMBER");
            LocalDate date = LocalDate.now();

            StandupRecord record = TestDataFactory.createRecord(1L, 1L, 100L, date, LocalDateTime.now().minusHours(2));
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            User user = TestDataFactory.createMember(1L, "成员");
            when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result.get(0).isEditable()).isFalse();
        }

        @Test
        @DisplayName("其他人不可编辑")
        void editable判定_其他人_应为false() {
            UserContext.set(2L, "MEMBER");
            LocalDate date = LocalDate.now();

            StandupRecord record = TestDataFactory.createRecord(1L, 1L, 100L, date, LocalDateTime.now().minusMinutes(10));
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            User user = TestDataFactory.createMember(1L, "成员");
            when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecords(100L, date);

            assertThat(result.get(0).isEditable()).isFalse();
        }
    }

    @Nested
    @DisplayName("获取已提交日期")
    class GetSubmittedDatesTest {

        @Test
        @DisplayName("返回已提交的日期列表")
        void 获取已提交日期_有记录_应返回去重日期列表() {
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);

            StandupRecord r1 = new StandupRecord();
            r1.setRecordDate(LocalDate.of(2024, 1, 15));

            StandupRecord r2 = new StandupRecord();
            r2.setRecordDate(LocalDate.of(2024, 1, 15));

            StandupRecord r3 = new StandupRecord();
            r3.setRecordDate(LocalDate.of(2024, 1, 20));

            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(r1, r2, r3));

            List<LocalDate> result = standupRecordService.getSubmittedDates(100L, start, end);

            assertThat(result).hasSize(2);
            assertThat(result).containsExactly(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20));
        }
    }

    @Nested
    @DisplayName("获取团队日期范围内站会记录")
    class GetTeamRecordsInRangeTest {

        @Test
        @DisplayName("返回指定日期范围的记录列表")
        void 获取日期范围记录_有记录_应返回列表() {
            UserContext.set(1L, "MEMBER");
            LocalDate start = LocalDate.of(2024, 1, 1);
            LocalDate end = LocalDate.of(2024, 1, 31);

            StandupRecord record = TestDataFactory.createRecord(1L, 1L, 100L, LocalDate.of(2024, 1, 15), LocalDateTime.now());
            when(standupRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));

            User user = TestDataFactory.createMember(1L, "成员");
            when(userMapper.selectBatchIds(List.of(1L))).thenReturn(List.of(user));

            List<StandupRecordVO> result = standupRecordService.getTeamRecordsInRange(100L, start, end);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRecordDate()).isEqualTo(LocalDate.of(2024, 1, 15));
        }
    }
}
