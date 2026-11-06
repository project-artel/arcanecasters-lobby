# ARTEL 전용 매치 설정

Lobby와 Game을 QA용 DB 및 Redis에 연결합니다. 두 서버에서 기존 환경 설정에 `qa` Spring 프로필을 추가하고 다음 환경 변수를 설정합니다.

| 서버 | 환경 변수 | 값 |
| --- | --- | --- |
| Lobby, Game | `ARTEL_QA_ENABLED` | `true` |
| Lobby, Game | `ARTEL_QA_ACCOUNTS` | 허용할 테스트 계정의 `memberId`를 쉼표로 구분한 목록 |
| Lobby | `ARTEL_QA_GAME_URL` | Lobby가 서버용 JWT를 보내는 단일 QA Game 인스턴스의 내부 URL |
| Game | `ARTEL_QA_BOT_ID` | QA DB에 덱이 설정된 봇의 음수 ID |

Unity Editor 또는 Development Build에서 QA Lobby에 접속해 허용된 계정으로 로그인합니다. `start_qa_bot_match`는 로그인 계정과 설정된 봇으로 매치를 시작합니다. `start_qa_duel`은 두 클라이언트에서 상대의 `memberId`를 입력하면 같은 활성 매치에 입장합니다. 두 테스트 계정에는 선택한 덱이 있어야 합니다.

`grant_magic`은 로그인한 계정의 보유 수량에 지정한 수량을 추가합니다. Game은 매치를 만들 때 보유 목록을 읽으므로 지급 후에 새 QA 매치를 시작합니다. `set_hand`에는 대상 테스트 계정이 보유한 마법 ID를 최대 6개 지정할 수 있습니다. 체력과 피해의 대상은 서버 오브젝트 ID이며, `spawn_unit` 응답에서 소환한 유닛의 ID를 확인할 수 있습니다.

전용 매치에서 실제 전투 결과 화면이 표시됩니다. Game은 해당 결과를 일반 매치 기록, 레이팅, 승리 수, 진행도에 저장하는 경로를 건너뜁니다.
