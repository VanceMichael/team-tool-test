USE standup;

INSERT IGNORE INTO user (username, password, nickname, role) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'ADMIN'),
('leader1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '组长张三', 'LEADER'),
('member1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '成员李四', 'MEMBER'),
('member2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '成员王五', 'MEMBER');

INSERT IGNORE INTO team (id, name, leader_id) VALUES
(1, '研发一组', 2);

INSERT IGNORE INTO team_member (team_id, user_id) VALUES
(1, 2),
(1, 3),
(1, 4);
