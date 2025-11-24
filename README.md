# 🏃‍♂️ TeamWalk (팀워크) - 함께 걷는 즐거움
실시간 그룹 경쟁 만보기 애플리케이션 > 친구, 가족, 동료와 함께 그룹을 만들어 걸음 수를 경쟁하고 건강한 습관을 만들어보세요.

## 📱 앱 소개 (Introduction)
TeamWalk는 혼자 하는 지루한 운동에서 벗어나, 지인들과 함께 경쟁하며 동기 부여를 얻을 수 있는 소셜 만보기 앱입니다. 안드로이드의 센서를 활용하여 정확한 걸음 수를 측정하며, 실시간으로 그룹 내 랭킹을 확인할 수 있습니다.

## 📸 스크린샷 (Screenshots)
|사진|사진|사진|사진|
|----|---|---|---|
|<img width="648" height="1296" alt="image" src="https://github.com/user-attachments/assets/4d8d1b23-1804-4087-9717-627cbd022574" />|<img width="648" height="1298" alt="image" src="https://github.com/user-attachments/assets/4ea2697d-fd3f-461f-b558-ecd9de8191cf" />|<img width="648" height="1290" alt="image" src="https://github.com/user-attachments/assets/2323e62e-9bbe-47a4-b267-e9529001eaa2" />|<img width="648" height="1307" alt="image" src="https://github.com/user-attachments/assets/8d0ff2a3-8099-40d3-aa72-a87c78b43a52" />|

## ✨ 주요 기능 (Key Features)
### 🏃‍♂️ 정확한 걸음 수 측정

- 실시간으로 걸음을 감지합니다.
- Foreground Service를 통해 앱이 백그라운드에 있거나 화면이 꺼져 있어도 측정이 중단되지 않습니다.

### 🏆 실시간 그룹 랭킹 시스템
누구나 쉽게 '방장'이 되어 그룹을 생성하고 코드를 공유할 수 있습니다.
참여 코드를 통해 그룹에 입장하면 실시간으로 멤버들의 걸음 수와 순위가 업데이트됩니다.

### 📅 자동 리셋 및 데이터 관리
자정(00:00)이 되면 걸음 수가 자동으로 0으로 초기화됩니다.
Firebase Firestore를 통해 데이터를 안전하게 저장하고 동기화합니다.

### 🔐 간편하고 안전한 로그인

Google Credential Manager를 도입하여 원터치 로그인을 지원합니다.
최신 안드로이드 보안 규정(Android 14+)을 준수합니다.

## 🛠 기술 스택 (Tech Stack)
🏗️ Architecture
Language: Kotlin

Design Pattern: Repository Pattern (데이터 계층과 UI 계층 분리)

UI: XML Layout, ViewBinding, Material Design

📚 Libraries
Android Jetpack:

Lifecycle: 생명주기 인식 컴포넌트

WorkManager / Service: 백그라운드 작업 처리

Asynchronous: Kotlin Coroutines (비동기 프로그래밍)

Firebase Authentication: 구글 로그인 및 사용자 인증

Firebase Firestore: 실시간 NoSQL 데이터베이스

ActivityResultContracts: 최신 권한 요청 API 사용

Credential Manager: 최신 통합 인증 방식

## ⚙️ 설치 및 실행 방법 (Getting Started)
현재 내부 테스트 중입니다. 추후에 나올 배포를 기대해 주세요!

👨‍💻 개발자 (Developer)
Name: Redish03

Email: mithmake@gmail.com

Role: 1인 개발 (기획, 디자인, 개발, 배포)
[Team-Walk Notion 기록](https://wonderful-report-e58.notion.site/Team-Walk-2b35b07568ed80918ec8c30576ab08ef?source=copy_link)
