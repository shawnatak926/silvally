-- 게임 규칙 v1.2. 전형 선택 및 제출 프리셋 (실제 학교 선발 기준과 무관).
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','AD01','ADMISSION_1',0,0,0,true,0,'입학 전형 선택','준비한 결과를 제출합니다.');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','AD02','ADMISSION_1',0,0,0,true,1,'1차 서류 제출','준비한 결과를 제출합니다.');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','AD03','ADMISSION_2',0,0,0,true,2,'스크래치 전형','준비한 결과를 제출합니다.');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','AD04','ADMISSION_2',0,0,0,true,3,'파이썬 전형과 기초능력','준비한 결과를 제출합니다.');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','AD05','ADMISSION_2',0,0,0,true,4,'학교 면접','준비한 결과를 제출합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD01','GENERAL','일반전형',0,false,'{}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD01','MEISTER','마이스터인재',0,false,'{}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD01','LOCAL','지역우선',0,false,'{}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD01','SOCIAL','사회통합',0,false,'{}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD02','BALANCED','균형 잡힌 서류',0,false,'{"essay": 80, "grades": 80, "record": 80}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD02','ACADEMIC','학업 중심 서류',0,false,'{"essay": 100, "grades": 100, "record": 0}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD02','ACTIVITY','활동 중심 서류',0,false,'{"essay": 50, "grades": 50, "record": 100}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD02','BASIC','기본 서류',0,false,'{"essay": 35, "grades": 35, "record": 35}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD03','PREPARED','준비한 결과 제출',0,false,'{"score": 95}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD03','BASIC','기초 결과 제출',0,false,'{"score": 40}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD04','PREPARED','준비한 결과 제출',0,false,'{"score": 95}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD04','BASIC','기초 결과 제출',0,false,'{"score": 40}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD05','PREPARED','준비한 결과 제출',0,false,'{"score": 95}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD05','BASIC','기초 결과 제출',0,false,'{"score": 40}');

-- 비포스쿨 활동: stats와 tags는 진로 추천 담당이 재사용하는 활동 근거입니다.
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','BS01','BEFORE_SCHOOL',0,0,0,true,1,'학교 탐색','7개 활동 중 2개를 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','LAB','실습동',0,false,'{"stats": {"P": 3, "A": 1}, "tags": ["programming"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','PROJECT_ROOM','프로젝트실',0,false,'{"stats": {"P": 2, "C": 2}, "tags": ["project", "teamwork"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','MAKER_ROOM','메이커실',0,false,'{"stats": {"I": 2, "P": 2}, "tags": ["maker"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','CLASSROOM','교실',0,false,'{"stats": {"A": 3, "C": 1}, "tags": ["study"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','DORMITORY','기숙사동',0,false,'{"stats": {"C": 2, "H": 2}, "tags": ["community"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','AUDITORIUM','강당',0,false,'{"stats": {"C": 3, "S": -2}, "tags": ["communication"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS01','FOOTBALL_FIELD','축구장',0,false,'{"stats": {"H": 4, "S": -3}, "tags": ["sports"]}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','BS02','BEFORE_SCHOOL',0,0,0,true,2,'전공 체험','8개 활동 중 2개를 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','WEB','웹 개발',0,false,'{"stats": {"F": 4, "P": 2}, "tags": ["web"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','BACKEND','백엔드 개발',0,false,'{"stats": {"B": 4, "P": 2}, "tags": ["backend"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','CROSS_PLATFORM','크로스플랫폼 개발',0,false,'{"stats": {"F": 2, "P": 3}, "tags": ["cross-platform"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','ANDROID','안드로이드 개발',0,false,'{"stats": {"P": 4, "B": 1}, "tags": ["android"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','IOS','iOS 개발',0,false,'{"stats": {"P": 4, "F": 1}, "tags": ["ios"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','GAME','게임 개발',0,false,'{"stats": {"P": 3, "I": 2}, "tags": ["game"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','NETWORK_SECURITY','네트워크와 보안',0,false,'{"stats": {"B": 3, "A": 2}, "tags": ["network", "security"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS02','AI_DESIGN','AI와 UI/UX',0,false,'{"stats": {"I": 3, "F": 2}, "tags": ["ai", "design"]}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','BS03','BEFORE_SCHOOL',0,0,0,true,3,'기초 프로그래밍','6개 활동 중 1개를 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','VARIABLES','변수와 자료형',0,false,'{"stats": {"P": 3}, "tags": ["programming"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','CONDITIONS','조건문',0,false,'{"stats": {"P": 3, "A": 1}, "tags": ["logic"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','LOOPS','반복문',0,false,'{"stats": {"P": 4}, "tags": ["algorithm"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','FUNCTIONS','함수',0,false,'{"stats": {"P": 3, "B": 1}, "tags": ["backend"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','DEBUGGING','디버깅',0,false,'{"stats": {"P": 3, "A": 2}, "tags": ["debugging"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS03','PAIR_PROGRAMMING','짝 프로그래밍',0,false,'{"stats": {"P": 2, "C": 3}, "tags": ["teamwork"]}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','BS04','BEFORE_SCHOOL',0,0,0,true,4,'팀 활동','6개 활동 중 1개를 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','LEAD','팀장',0,false,'{"stats": {"C": 4, "A": 1}, "tags": ["leadership"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','PLAN','기획',0,false,'{"stats": {"A": 3, "C": 2}, "tags": ["planning"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','DESIGN','디자인',0,false,'{"stats": {"F": 3, "C": 1}, "tags": ["design"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','DEVELOP','개발',0,false,'{"stats": {"P": 3, "B": 2}, "tags": ["project", "backend"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','TEST','테스트',0,false,'{"stats": {"P": 2, "A": 3}, "tags": ["debugging"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS04','PRESENT','발표',0,false,'{"stats": {"C": 4, "S": -2}, "tags": ["communication"]}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','BS05','BEFORE_SCHOOL',0,0,0,true,5,'진로 상담','6개 활동 중 1개를 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','WEB_MOBILE','웹과 모바일 상담',0,false,'{"stats": {"F": 3, "P": 1}, "tags": ["web", "android", "ios"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','SERVER','서버 개발 상담',0,false,'{"stats": {"B": 3, "P": 1}, "tags": ["backend"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','AI','AI 개발 상담',0,false,'{"stats": {"I": 3, "A": 1}, "tags": ["ai"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','GAME','게임 개발 상담',0,false,'{"stats": {"P": 3, "I": 1}, "tags": ["game"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','SECURITY','네트워크와 보안 상담',0,false,'{"stats": {"B": 2, "A": 2}, "tags": ["network", "security"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','BS05','UX','UI/UX 상담',0,false,'{"stats": {"F": 2, "C": 2}, "tags": ["design"]}');

-- 학년 전환 필수 선택: 2학년 진급 시 전공, 3학년 진급 시 트랙. focus는 턴 종료 시 +1 성장하는 능력치.
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','MAJOR','SOFTWARE','소프트웨어개발과','{"focus": ["P", "B"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','MAJOR','EMBEDDED','임베디드소프트웨어과','{"focus": ["P", "I"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','MAJOR','SECURITY','정보보안과','{"focus": ["B", "A"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','TRACK','WEB','웹 서비스 트랙','{"focus": ["F", "B"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','TRACK','MOBILE','모바일 앱 트랙','{"focus": ["P", "F"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','TRACK','AI','AI 트랙','{"focus": ["I", "A"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','TRACK','GAME','게임 트랙','{"focus": ["P", "I"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','TRACK','NETWORK','네트워크·보안 트랙','{"focus": ["B", "A"]}');

-- 방과후 자격증 과정 (실제 자격증 시험 기준과 무관). 출석 2회 후 응시, 점수 = exam 능력치 평균×1.2+20±5.
-- validYears 0은 만료 없음, 1 이상이면 취득 학년 + validYears 학년이 끝나면 만료.
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COURSE','INFO_CRAFTSMAN','정보처리 방과후','{"certificate": "정보처리기능사", "minYear": 1, "requires": {"P": 25}, "exam": ["P", "A"], "passScore": 60, "validYears": 0}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COURSE','WEB_DESIGN','웹디자인 방과후','{"certificate": "웹디자인개발기능사", "minYear": 1, "requires": {"F": 15}, "exam": ["F", "P"], "passScore": 60, "validYears": 0}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COURSE','ENGLISH_SPEAKING','영어 말하기 방과후','{"certificate": "영어 말하기 평가", "minYear": 1, "requires": {"C": 20}, "exam": ["C", "A"], "passScore": 55, "validYears": 1}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COURSE','LINUX_MASTER','리눅스 방과후','{"certificate": "리눅스마스터 2급", "minYear": 2, "requires": {"B": 25}, "exam": ["B", "A"], "passScore": 65, "validYears": 0}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COURSE','NETWORK_ADMIN','네트워크 방과후','{"certificate": "네트워크관리사 2급", "minYear": 2, "requires": {"B": 20, "A": 30}, "exam": ["B", "A"], "passScore": 65, "validYears": 0}');

-- 졸업 엔딩: 판정은 GraduationPolicy, 제목·본문은 이 카탈로그를 사용합니다.
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','ENDING','EMPLOYED','희망 직무 취업','{"body": "졸업과 함께 희망한 직무의 오퍼를 수락하고 사회에 첫걸음을 내딛었습니다."}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','ENDING','EMPLOYED_MISMATCH','다른 길로 취업','{"body": "희망 직무와는 다르지만 받은 오퍼를 수락해 새로운 분야에서 커리어를 시작했습니다."}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','ENDING','UNEMPLOYED','미취업 졸업','{"body": "아직 정해진 곳 없이 졸업했습니다. 쌓아 온 경험이 다음 도전의 밑거름이 될 것입니다."}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','ENDING','UNIVERSITY','대학 진학','{"body": "더 깊은 공부를 위해 대학 진학의 길을 선택했습니다."}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','ENDING','INDIE_DEV','개인 개발자','{"body": "포트폴리오의 작품을 발판 삼아 직접 만든 서비스로 승부하는 개인 개발자가 되었습니다."}');
