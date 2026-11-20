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
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','AD05','ANSWER','학교 면접 답변 제출',0,false,'{}');

-- 재학 사건: 효과와 기본 장면을 함께 저장합니다.
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','S01','SCHOOL',1,1,1,false,1,'처음 만난 팀원과 협업','현재 시기에 할 활동을 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S01','COOPERATE','함께 작은 프로그램 만들기',1,false,'{"stats": {"P": 2, "C": 3}, "tags": ["teamwork", "project"], "scene": "역할을 나누고 함께 프로그램을 완성했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S01','REST','서로 알아가며 휴식',0,false,'{"stats": {"C": 1, "S": -2}, "tags": ["community"], "scene": "친구와 대화하며 긴장을 풀었습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','S02','SCHOOL',2,2,2,false,2,'전공 프로젝트의 문제 해결','현재 시기에 할 활동을 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S02','PRACTICE','작은 실험으로 문제 분석',1,false,'{"stats": {"P": 3, "I": 2}, "tags": ["debugging"], "scene": "실험 결과를 비교하며 문제의 원인을 찾았습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S02','DISCUSS','동료와 해결책 토론',1,false,'{"stats": {"C": 3, "A": 2}, "tags": ["teamwork"], "scene": "서로 다른 가설을 검토하고 해결책을 정리했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','S03','SCHOOL',3,2,2,false,3,'현장 진출을 위한 준비','현재 시기에 할 활동을 선택합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S03','PORTFOLIO','프로젝트 설명 정리',1,false,'{"stats": {"P": 2, "C": 2}, "tags": ["project"], "scene": "직접 만든 프로젝트의 문제와 해결 과정을 정리했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','S03','PRESENT','동료 앞에서 발표 연습',1,false,'{"stats": {"C": 3, "S": -1}, "tags": ["communication"], "scene": "발표 피드백을 받고 설명을 다듬었습니다."}');

-- 최신 학교 일정: 정규 24턴/학기 12턴, 방학 캠프 실습은 별도 2턴.
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y1_S1','SCHOOL',1,1,12,true,0,'1학년 1학기 집중 목표','1학년 1학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S1','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S1','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S1','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y1_S2','SCHOOL',1,13,24,true,0,'1학년 2학기 집중 목표','1학년 2학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S2','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S2','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y1_S2','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y1_SUMMER','SCHOOL',1,12,12,true,0,'여름방학','여름방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y1_SUMMER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y1_SUMMER','CAMP','전공강화캠프 참가',0,false,'{"stats": {}, "tags": ["camp"], "scene": "전공강화캠프의 방학 실습 2턴을 시작했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y1_WINTER','SCHOOL',1,24,24,true,0,'겨울방학','겨울방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y1_WINTER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y1_WINTER','CAMP','전공강화캠프 참가',0,false,'{"stats": {}, "tags": ["camp"], "scene": "전공강화캠프의 방학 실습 2턴을 시작했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y2_S1','SCHOOL',2,1,12,true,0,'2학년 1학기 집중 목표','2학년 1학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S1','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S1','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S1','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y2_S2','SCHOOL',2,13,24,true,0,'2학년 2학기 집중 목표','2학년 2학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S2','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S2','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y2_S2','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y2_SUMMER','SCHOOL',2,12,12,true,0,'여름방학','여름방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y2_SUMMER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y2_SUMMER','CAMP','전공강화캠프 참가',0,false,'{"stats": {}, "tags": ["camp"], "scene": "전공강화캠프의 방학 실습 2턴을 시작했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y2_WINTER','SCHOOL',2,24,24,true,0,'겨울방학','겨울방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y2_WINTER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y2_WINTER','CAMP','전공강화캠프 참가',0,false,'{"stats": {}, "tags": ["camp"], "scene": "전공강화캠프의 방학 실습 2턴을 시작했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y3_S1','SCHOOL',3,1,12,true,0,'3학년 1학기 집중 목표','3학년 1학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S1','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S1','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S1','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','FOCUS_Y3_S2','SCHOOL',3,13,24,true,0,'3학년 2학기 집중 목표','3학년 2학기 집중 목표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S2','PROGRAMMING','프로그래밍 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S2','COMMUNICATION','협업·의사소통 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','FOCUS_Y3_S2','ACADEMIC','학업 집중',0,false,'{"stats": {}, "tags": ["semester-focus"], "scene": "학기 집중 목표를 정했습니다. 정규 턴 진행 시 성장에 반영됩니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y3_SUMMER','SCHOOL',3,12,12,true,0,'여름방학','여름방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y3_SUMMER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','VACATION_Y3_WINTER','SCHOOL',3,24,24,true,0,'겨울방학','겨울방학 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','VACATION_Y3_WINTER','REST','휴식',0,false,'{"stats": {"H": 5, "S": -3}, "tags": ["vacation"], "scene": "방학 동안 쉬며 다음 시기를 준비했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','NARSHA_START','SCHOOL',1,8,8,false,0,'나르샤 시작','나르샤 시작 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_START','JOIN','팀 프로젝트 참가',0,false,'{"stats": {}, "tags": ["narsha"], "scene": "나르샤 팀 프로젝트에 참가했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_START','SKIP','이번 프로젝트 미참가',0,false,'{"stats": {}, "tags": [], "scene": "이번 나르샤 프로젝트에는 참가하지 않았습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','NARSHA_WRAP','SCHOOL',1,23,23,true,0,'1학년 나르샤 마무리','1학년 나르샤 마무리 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_WRAP','SUBMIT','프로젝트 결과 제출',0,false,'{"stats": {"P": 2, "C": 2}, "tags": ["narsha", "project"], "scene": "1학년 나르샤 프로젝트를 마무리했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','NARSHA_RESUME','SCHOOL',2,2,2,true,0,'2학년 나르샤 시작','2학년 나르샤는 1학년 프로젝트와 별도의 새 프로젝트입니다. 1학년 참가 여부와 무관하게 새 주제로 시작하거나 이번에는 참가하지 않을 수 있습니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_RESUME','RESUME','새 프로젝트 시작',0,false,'{"stats": {}, "tags": ["narsha"], "scene": "2학년 나르샤 새 프로젝트를 시작했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_RESUME','SKIP','이번 프로젝트 미참가',0,false,'{"stats": {}, "tags": [], "scene": "이번 학년 나르샤 프로젝트에는 참가하지 않았습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','NARSHA_RANK','SCHOOL',2,22,22,true,0,'나르샤 순위 발표','나르샤 순위 발표 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','NARSHA_RANK','CHECK','팀 순위 확인',0,false,'{"stats": {}, "tags": ["narsha"], "scene": "게임 규칙에 따라 나르샤 팀 순위를 확정했습니다."}');
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','SOFTWAREWAVE','SCHOOL',2,23,23,false,0,'소프트웨이브 참가 선택','소프트웨이브 참가 선택 선택을 확정합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','SOFTWAREWAVE','ATTEND','소프트웨이브 참가',0,false,'{"stats": {"C": 3}, "tags": ["softwarewave"], "scene": "나르샤 상위 팀 자격으로 소프트웨이브에 참가했습니다."}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','SOFTWAREWAVE','SKIP','참가하지 않음',0,false,'{"stats": {}, "tags": [], "scene": "이번 소프트웨이브에는 참가하지 않기로 했습니다."}');

-- 가상 게임 현장학습: 3학년 3턴 신청, 해외 영어50 기준. 미달은 정상 미선발 결과입니다.
INSERT INTO event_def (rules_version,event_code,phase,school_year,min_turn,max_turn,required,order_no,title,situation) VALUES ('v1.2','J01','SCHOOL',3,3,3,true,10,'현장학습 신청','국내 현장실습 또는 실리콘밸리 현장학습에 신청합니다. 해외는 영어 50점 이상이 필요합니다.');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','J01','DOMESTIC','국내 현장실습 신청',0,false,'{"stats":{},"tags":["field-learning","domestic"]}');
INSERT INTO event_choice (rules_version,event_code,choice_code,label,ap_cost,heavy,effect_json) VALUES ('v1.2','J01','SILICON_VALLEY','실리콘밸리 현장학습 신청',0,false,'{"stats":{},"tags":["field-learning","overseas"]}');

-- 프로젝트: role은 포트폴리오 환산 직무, focus는 작업 스냅샷에 반영하는 능력치, durationTurns는 1학년 24턴 기준 작업 기간입니다.
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','PROJECT','WEB_PORTFOLIO_SITE','포트폴리오 웹사이트','{"role": "WEB", "minYear": 1, "durationTurns": 10, "workRequired": 2, "focus": ["F", "P"], "passQuality": 45}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','PROJECT','REST_API_SERVER','REST API 서버','{"role": "BACKEND", "minYear": 1, "durationTurns": 10, "workRequired": 2, "focus": ["B", "P"], "passQuality": 45}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','PROJECT','ANDROID_NOTE_APP','안드로이드 메모 앱','{"role": "ANDROID", "minYear": 2, "durationTurns": 12, "workRequired": 3, "focus": ["P", "F"], "passQuality": 50}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','PROJECT','MINI_GAME','미니 게임','{"role": "GAME", "minYear": 2, "durationTurns": 12, "workRequired": 3, "focus": ["P", "I"], "passQuality": 50}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','PROJECT','CHATBOT','AI 챗봇','{"role": "AI", "minYear": 3, "durationTurns": 12, "workRequired": 3, "focus": ["I", "A"], "passQuality": 55}');

-- 기업: deadlineTurn은 1학년 1턴을 1로 센 누적 턴이며 1학년 24턴 기준입니다. (2학년 1턴 = 25, 3학년 1턴 = 49, 3학년 24턴 = 72)
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COMPANY','BYTE_WORKS','바이트웍스','{"role": "BACKEND", "minYear": 2, "deadlineTurn": 58, "docPass": 30, "codingPass": 50, "interviewPass": 50, "offerPass": 55, "codingStats": ["P", "B"], "interviewStats": ["C", "A"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COMPANY','PIXEL_LAB','픽셀랩','{"role": "WEB", "minYear": 2, "deadlineTurn": 58, "docPass": 30, "codingPass": 45, "interviewPass": 50, "offerPass": 55, "codingStats": ["F", "P"], "interviewStats": ["C", "A"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COMPANY','DROID_FACTORY','드로이드팩토리','{"role": "ANDROID", "minYear": 3, "deadlineTurn": 66, "docPass": 35, "codingPass": 55, "interviewPass": 50, "offerPass": 60, "codingStats": ["P", "F"], "interviewStats": ["C", "A"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COMPANY','DUNGEON_GAMES','던전게임즈','{"role": "GAME", "minYear": 3, "deadlineTurn": 66, "docPass": 35, "codingPass": 55, "interviewPass": 50, "offerPass": 60, "codingStats": ["P", "I"], "interviewStats": ["C", "I"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','COMPANY','DEEP_MIND_KR','딥마인드코리아','{"role": "AI", "minYear": 3, "deadlineTurn": 70, "docPass": 40, "codingPass": 60, "interviewPass": 55, "offerPass": 65, "codingStats": ["I", "A"], "interviewStats": ["A", "C"]}');

-- 직무: order는 추천 동점 처리에 쓰는 고정 순번, stats는 직무 적합 능력치 가중치, tags는 비포스쿨 활동 태그와 맞춰 보는 값입니다.
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','WEB','웹 프론트엔드','{"order": 1, "stats": {"F": 3, "P": 1}, "tags": ["web", "design"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','BACKEND','백엔드','{"order": 2, "stats": {"B": 3, "P": 1}, "tags": ["backend", "algorithm", "logic"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','CROSS_PLATFORM','크로스플랫폼','{"order": 3, "stats": {"F": 2, "P": 2}, "tags": ["cross-platform", "web"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','ANDROID','안드로이드','{"order": 4, "stats": {"P": 3, "B": 1}, "tags": ["android"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','IOS','iOS','{"order": 5, "stats": {"P": 3, "F": 1}, "tags": ["ios"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','GAME','게임 개발','{"order": 6, "stats": {"P": 2, "I": 2}, "tags": ["game", "maker"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','NETWORK','네트워크','{"order": 7, "stats": {"B": 3, "A": 1}, "tags": ["network"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','SECURITY','보안','{"order": 8, "stats": {"B": 2, "A": 2}, "tags": ["security", "debugging"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','AI','AI 개발','{"order": 9, "stats": {"I": 3, "A": 1}, "tags": ["ai", "study"]}');
INSERT INTO catalog_def (rules_version,kind,code,title,config_json) VALUES ('v1.2','CAREER_ROLE','DESIGN','UI/UX 디자인','{"order": 10, "stats": {"F": 2, "C": 2}, "tags": ["design", "communication"]}');
