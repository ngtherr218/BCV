# Database schema after — live read-only snapshot

Captured UTC: 2026-11-12T09:26:55.8886204+00:00. Source: local `vietnam_explorer_dev_v2`. Isolation: REPEATABLE READ, READ ONLY. No user rows exported.

| STT | Bảng | PK | FK | Số bản ghi | Chức năng |
|---|---|---|---|---|---|
| 1 | app_user | PRIMARY KEY (user_id) |  | 72 | Tài khoản |
| 2 | card | PRIMARY KEY (card_id) | FOREIGN KEY (place_id) REFERENCES place(place_id) | 135 | Danh mục thẻ |
| 3 | collection | PRIMARY KEY (collection_id) |  | 7 | Bộ sưu tập |
| 4 | collection_card | PRIMARY KEY (collection_card_id) | FOREIGN KEY (card_id) REFERENCES card(card_id); FOREIGN KEY (collection_id) REFERENCES collection(collection_id) | 140 | Thẻ thuộc bộ |
| 5 | community_media | PRIMARY KEY (media_id) | FOREIGN KEY (place_id) REFERENCES place(place_id); FOREIGN KEY (reviewed_by) REFERENCES app_user(user_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 2 | Media cộng đồng có kiểm duyệt |
| 6 | explorer_reward_config | PRIMARY KEY (singleton) | FOREIGN KEY (updated_by) REFERENCES app_user(user_id) | 1 | Cần đối chiếu module |
| 7 | explorer_score_event | PRIMARY KEY (score_event_id) | FOREIGN KEY (place_id) REFERENCES place(place_id) ON UPDATE RESTRICT ON DELETE RESTRICT; FOREIGN KEY (region_id) REFERENCES region(region_id) ON UPDATE RESTRICT ON DELETE RESTRICT; FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON UPDATE RESTRICT ON DELETE RESTRICT | 203 | Sổ điểm thành tích |
| 8 | interest_group | PRIMARY KEY (interest_group_id) |  | 18 | Nhóm sở thích |
| 9 | item | PRIMARY KEY (item_id) |  | 2 | Vật phẩm |
| 10 | journey | PRIMARY KEY (journey_id) | FOREIGN KEY (destination_place_id) REFERENCES place(place_id); FOREIGN KEY (start_place_id) REFERENCES place(place_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 82 | Hành trình |
| 11 | media_report | PRIMARY KEY (report_id) | FOREIGN KEY (media_id) REFERENCES community_media(media_id) ON DELETE CASCADE; FOREIGN KEY (reporter_id) REFERENCES app_user(user_id); FOREIGN KEY (resolved_by) REFERENCES app_user(user_id) | 0 | Báo cáo nội dung |
| 12 | notification | PRIMARY KEY (notification_id) | FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 317 | Thông báo |
| 13 | place | PRIMARY KEY (place_id) | FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id); FOREIGN KEY (region_id) REFERENCES region(region_id) | 312 | Địa danh |
| 14 | place_feature | PRIMARY KEY (place_feature_id) | FOREIGN KEY (place_id) REFERENCES place(place_id); FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id) | 312 | Cụm địa danh theo model |
| 15 | place_feature_value | PRIMARY KEY (place_feature_id, criterion_id) | FOREIGN KEY (criterion_id) REFERENCES recommendation_criterion(criterion_id); FOREIGN KEY (place_feature_id) REFERENCES place_feature(place_feature_id) | 10920 | Vector đặc trưng |
| 16 | place_image | PRIMARY KEY (place_image_id) | FOREIGN KEY (place_id) REFERENCES place(place_id) | 68 | Ảnh chính thức |
| 17 | place_type | PRIMARY KEY (place_type_id) |  | 22 | Loại địa danh |
| 18 | place_type_feature | PRIMARY KEY (place_type_id, code) | FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id) ON DELETE CASCADE | 0 | Đặc trưng mặc định theo loại |
| 19 | quest | PRIMARY KEY (quest_id) | FOREIGN KEY (reward_item_id) REFERENCES item(item_id) | 43 | Nhiệm vụ |
| 20 | quest_progress | PRIMARY KEY (quest_progress_id) | FOREIGN KEY (quest_id) REFERENCES quest(quest_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 333 | Tiến độ và chi thưởng theo chu kỳ |
| 21 | recommendation_config | PRIMARY KEY (recommendation_config_id) |  | 1 | Phiên bản mô hình |
| 22 | recommendation_criterion | PRIMARY KEY (criterion_id) |  | 40 | Tiêu chí và mã đặc trưng |
| 23 | recommendation_rating_snapshot | PRIMARY KEY (recommendation_config_id, place_id) | FOREIGN KEY (place_id) REFERENCES place(place_id); FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id) | 312 | Đánh giá theo phiên bản |
| 24 | recommendation_weight | PRIMARY KEY (recommendation_config_id, criterion_id) | FOREIGN KEY (criterion_id) REFERENCES recommendation_criterion(criterion_id); FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id) | 5 | Trọng số gợi ý |
| 25 | region | PRIMARY KEY (region_id) |  | 63 | Vùng địa danh |
| 26 | spatial_ref_sys | PRIMARY KEY (srid) |  | 8500 | Hệ tọa độ PostGIS |
| 27 | step_data | PRIMARY KEY (step_data_id) | FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 72 | Dữ liệu bước đồng bộ |
| 28 | user_card | PRIMARY KEY (user_card_id) | FOREIGN KEY (card_id) REFERENCES card(card_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 42 | Thẻ sở hữu |
| 29 | user_discovered_place | PRIMARY KEY (user_discovered_place_id) | FOREIGN KEY (place_id) REFERENCES place(place_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 74 | Lịch sử khám phá đầu tiên |
| 30 | user_item | PRIMARY KEY (user_item_id) | FOREIGN KEY (item_id) REFERENCES item(item_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 43 | Kho vật phẩm |
| 31 | user_preference | PRIMARY KEY (user_preference_id) | FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 69 | Hồ sơ vận động |
| 32 | user_preference_interest | PRIMARY KEY (user_preference_id, interest_group_id) | FOREIGN KEY (interest_group_id) REFERENCES interest_group(interest_group_id); FOREIGN KEY (user_preference_id) REFERENCES user_preference(user_preference_id) ON DELETE CASCADE | 96 | Nhóm sở thích cá nhân |
| 33 | user_preference_place_type | PRIMARY KEY (user_preference_id, place_type_id) | FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id); FOREIGN KEY (user_preference_id) REFERENCES user_preference(user_preference_id) | 7 | Sở thích loại cũ |
| 34 | virtual_location | PRIMARY KEY (virtual_location_id) | FOREIGN KEY (nearest_place_id) REFERENCES place(place_id); FOREIGN KEY (region_id) REFERENCES region(region_id); FOREIGN KEY (user_id) REFERENCES app_user(user_id) | 69 | Vị trí ảo |

## Chi tiết cột, PK/FK, UNIQUE, CHECK và index

columns: name, exact SQL type (including precision/length), NOT NULL, default, identity kind, generated kind, collation. constraints: name, kind, PostgreSQL definition (including FK actions). indexes: name, full definition.

```json
[
  {
    "table": "app_user",
    "count": "72",
    "columns": [
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "full_name",
        "character varying(100)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "email",
        "character varying(255)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "password_hash",
        "character varying(255)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "avatar",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "role",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "created_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "last_login_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "media_upload_blocked",
        "boolean",
        "true",
        "false",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "app_user_created_at_not_null",
        "n",
        "NOT NULL created_at"
      ],
      [
        "app_user_email_key",
        "u",
        "UNIQUE (email)"
      ],
      [
        "app_user_email_not_null",
        "n",
        "NOT NULL email"
      ],
      [
        "app_user_full_name_not_null",
        "n",
        "NOT NULL full_name"
      ],
      [
        "app_user_media_upload_blocked_not_null",
        "n",
        "NOT NULL media_upload_blocked"
      ],
      [
        "app_user_password_hash_not_null",
        "n",
        "NOT NULL password_hash"
      ],
      [
        "app_user_pkey",
        "p",
        "PRIMARY KEY (user_id)"
      ],
      [
        "app_user_role_not_null",
        "n",
        "NOT NULL role"
      ],
      [
        "app_user_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "app_user_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "app_user_email_key",
        "CREATE UNIQUE INDEX app_user_email_key ON public.app_user USING btree (email)"
      ],
      [
        "app_user_pkey",
        "CREATE UNIQUE INDEX app_user_pkey ON public.app_user USING btree (user_id)"
      ]
    ]
  },
  {
    "table": "card",
    "count": "135",
    "columns": [
      [
        "card_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(150)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "image_key",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "card_card_id_not_null",
        "n",
        "NOT NULL card_id"
      ],
      [
        "card_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "card_pkey",
        "p",
        "PRIMARY KEY (card_id)"
      ],
      [
        "card_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "card_place_id_key",
        "u",
        "UNIQUE (place_id)"
      ],
      [
        "card_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "card_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "card_pkey",
        "CREATE UNIQUE INDEX card_pkey ON public.card USING btree (card_id)"
      ],
      [
        "card_place_id_key",
        "CREATE UNIQUE INDEX card_place_id_key ON public.card USING btree (place_id)"
      ]
    ]
  },
  {
    "table": "collection",
    "count": "7",
    "columns": [
      [
        "collection_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(150)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "collection_collection_id_not_null",
        "n",
        "NOT NULL collection_id"
      ],
      [
        "collection_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "collection_pkey",
        "p",
        "PRIMARY KEY (collection_id)"
      ],
      [
        "collection_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "collection_pkey",
        "CREATE UNIQUE INDEX collection_pkey ON public.collection USING btree (collection_id)"
      ]
    ]
  },
  {
    "table": "collection_card",
    "count": "140",
    "columns": [
      [
        "collection_card_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "collection_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "card_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "collection_card_card_id_fkey",
        "f",
        "FOREIGN KEY (card_id) REFERENCES card(card_id)"
      ],
      [
        "collection_card_card_id_not_null",
        "n",
        "NOT NULL card_id"
      ],
      [
        "collection_card_collection_card_id_not_null",
        "n",
        "NOT NULL collection_card_id"
      ],
      [
        "collection_card_collection_id_card_id_key",
        "u",
        "UNIQUE (collection_id, card_id)"
      ],
      [
        "collection_card_collection_id_fkey",
        "f",
        "FOREIGN KEY (collection_id) REFERENCES collection(collection_id)"
      ],
      [
        "collection_card_collection_id_not_null",
        "n",
        "NOT NULL collection_id"
      ],
      [
        "collection_card_pkey",
        "p",
        "PRIMARY KEY (collection_card_id)"
      ]
    ],
    "indexes": [
      [
        "collection_card_card_idx",
        "CREATE INDEX collection_card_card_idx ON public.collection_card USING btree (card_id)"
      ],
      [
        "collection_card_collection_id_card_id_key",
        "CREATE UNIQUE INDEX collection_card_collection_id_card_id_key ON public.collection_card USING btree (collection_id, card_id)"
      ],
      [
        "collection_card_pkey",
        "CREATE UNIQUE INDEX collection_card_pkey ON public.collection_card USING btree (collection_card_id)"
      ]
    ]
  },
  {
    "table": "community_media",
    "count": "2",
    "columns": [
      [
        "media_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "caption",
        "character varying(500)",
        "true",
        "\u0027\u0027::character varying",
        "",
        "",
        "default"
      ],
      [
        "mime_type",
        "character varying(40)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "media_data",
        "bytea",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "\u0027PENDING\u0027::character varying",
        "",
        "",
        "default"
      ],
      [
        "review_note",
        "character varying(500)",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "reviewed_by",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "created_at",
        "timestamp with time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "reviewed_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "first_approved_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "file_deleted_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "delete_after",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "community_media_caption_not_null",
        "n",
        "NOT NULL caption"
      ],
      [
        "community_media_created_at_not_null",
        "n",
        "NOT NULL created_at"
      ],
      [
        "community_media_media_id_not_null",
        "n",
        "NOT NULL media_id"
      ],
      [
        "community_media_mime_type_not_null",
        "n",
        "NOT NULL mime_type"
      ],
      [
        "community_media_pkey",
        "p",
        "PRIMARY KEY (media_id)"
      ],
      [
        "community_media_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "community_media_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "community_media_reviewed_by_fkey",
        "f",
        "FOREIGN KEY (reviewed_by) REFERENCES app_user(user_id)"
      ],
      [
        "community_media_status_check",
        "c",
        "CHECK (((status)::text = ANY ((ARRAY[\u0027PENDING\u0027::character varying, \u0027APPROVED\u0027::character varying, \u0027REJECTED\u0027::character varying, \u0027FLAGGED\u0027::character varying, \u0027REMOVED\u0027::character varying, \u0027EXPIRED\u0027::character varying])::text[])))"
      ],
      [
        "community_media_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "community_media_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "community_media_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "community_media_pkey",
        "CREATE UNIQUE INDEX community_media_pkey ON public.community_media USING btree (media_id)"
      ],
      [
        "community_media_place_status_idx",
        "CREATE INDEX community_media_place_status_idx ON public.community_media USING btree (place_id, status)"
      ],
      [
        "community_media_review_idx",
        "CREATE INDEX community_media_review_idx ON public.community_media USING btree (status, created_at)"
      ]
    ]
  },
  {
    "table": "explorer_reward_config",
    "count": "1",
    "columns": [
      [
        "singleton",
        "boolean",
        "true",
        "true",
        "",
        "",
        "NULL"
      ],
      [
        "quest_points",
        "integer",
        "true",
        "20",
        "",
        "",
        "NULL"
      ],
      [
        "journey_points",
        "integer",
        "true",
        "50",
        "",
        "",
        "NULL"
      ],
      [
        "first_place_points",
        "integer",
        "true",
        "100",
        "",
        "",
        "NULL"
      ],
      [
        "revision",
        "bigint",
        "true",
        "1",
        "",
        "",
        "NULL"
      ],
      [
        "updated_at",
        "timestamp with time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "updated_by",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "discovery_points",
        "integer",
        "true",
        "150",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "explorer_reward_config_discovery_points_not_null",
        "n",
        "NOT NULL discovery_points"
      ],
      [
        "explorer_reward_config_first_place_points_check",
        "c",
        "CHECK (((first_place_points \u003E= 0) AND (first_place_points \u003C= 100000)))"
      ],
      [
        "explorer_reward_config_first_place_points_not_null",
        "n",
        "NOT NULL first_place_points"
      ],
      [
        "explorer_reward_config_journey_points_check",
        "c",
        "CHECK (((journey_points \u003E= 0) AND (journey_points \u003C= 100000)))"
      ],
      [
        "explorer_reward_config_journey_points_not_null",
        "n",
        "NOT NULL journey_points"
      ],
      [
        "explorer_reward_config_pkey",
        "p",
        "PRIMARY KEY (singleton)"
      ],
      [
        "explorer_reward_config_quest_points_check",
        "c",
        "CHECK (((quest_points \u003E= 0) AND (quest_points \u003C= 100000)))"
      ],
      [
        "explorer_reward_config_quest_points_not_null",
        "n",
        "NOT NULL quest_points"
      ],
      [
        "explorer_reward_config_revision_not_null",
        "n",
        "NOT NULL revision"
      ],
      [
        "explorer_reward_config_singleton_check",
        "c",
        "CHECK (singleton)"
      ],
      [
        "explorer_reward_config_singleton_not_null",
        "n",
        "NOT NULL singleton"
      ],
      [
        "explorer_reward_config_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ],
      [
        "explorer_reward_config_updated_by_fkey",
        "f",
        "FOREIGN KEY (updated_by) REFERENCES app_user(user_id)"
      ],
      [
        "reward_discovery_range",
        "c",
        "CHECK (((discovery_points \u003E= 0) AND (discovery_points \u003C= 200000)))"
      ]
    ],
    "indexes": [
      [
        "explorer_reward_config_pkey",
        "CREATE UNIQUE INDEX explorer_reward_config_pkey ON public.explorer_reward_config USING btree (singleton)"
      ]
    ]
  },
  {
    "table": "explorer_score_event",
    "count": "203",
    "columns": [
      [
        "score_event_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "event_type",
        "character varying(30)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "source_key",
        "character varying(100)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "place_id",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "region_id",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "points",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "rule_version",
        "character varying(30)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "occurred_at",
        "timestamp with time zone",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "explorer_score_event_event_type_not_null",
        "n",
        "NOT NULL event_type"
      ],
      [
        "explorer_score_event_occurred_at_not_null",
        "n",
        "NOT NULL occurred_at"
      ],
      [
        "explorer_score_event_pkey",
        "p",
        "PRIMARY KEY (score_event_id)"
      ],
      [
        "explorer_score_event_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id) ON UPDATE RESTRICT ON DELETE RESTRICT"
      ],
      [
        "explorer_score_event_points_check",
        "c",
        "CHECK ((points \u003E 0))"
      ],
      [
        "explorer_score_event_points_not_null",
        "n",
        "NOT NULL points"
      ],
      [
        "explorer_score_event_region_id_fkey",
        "f",
        "FOREIGN KEY (region_id) REFERENCES region(region_id) ON UPDATE RESTRICT ON DELETE RESTRICT"
      ],
      [
        "explorer_score_event_rule_version_not_null",
        "n",
        "NOT NULL rule_version"
      ],
      [
        "explorer_score_event_score_event_id_not_null",
        "n",
        "NOT NULL score_event_id"
      ],
      [
        "explorer_score_event_source_key_not_null",
        "n",
        "NOT NULL source_key"
      ],
      [
        "explorer_score_event_type_check",
        "c",
        "CHECK (((event_type)::text = ANY ((ARRAY[\u0027FIRST_PLACE\u0027::character varying, \u0027JOURNEY\u0027::character varying, \u0027QUEST\u0027::character varying, \u0027DISCOVERY\u0027::character varying])::text[])))"
      ],
      [
        "explorer_score_event_user_id_event_type_source_key_key",
        "u",
        "UNIQUE (user_id, event_type, source_key)"
      ],
      [
        "explorer_score_event_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON UPDATE RESTRICT ON DELETE RESTRICT"
      ],
      [
        "explorer_score_event_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "explorer_score_event_pkey",
        "CREATE UNIQUE INDEX explorer_score_event_pkey ON public.explorer_score_event USING btree (score_event_id)"
      ],
      [
        "explorer_score_event_user_id_event_type_source_key_key",
        "CREATE UNIQUE INDEX explorer_score_event_user_id_event_type_source_key_key ON public.explorer_score_event USING btree (user_id, event_type, source_key)"
      ],
      [
        "explorer_score_region_time_user_idx",
        "CREATE INDEX explorer_score_region_time_user_idx ON public.explorer_score_event USING btree (region_id, occurred_at, user_id)"
      ],
      [
        "explorer_score_time_user_idx",
        "CREATE INDEX explorer_score_time_user_idx ON public.explorer_score_event USING btree (occurred_at, user_id)"
      ]
    ]
  },
  {
    "table": "interest_group",
    "count": "18",
    "columns": [
      [
        "interest_group_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "code",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "name",
        "character varying(100)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "interest_group_code_key",
        "u",
        "UNIQUE (code)"
      ],
      [
        "interest_group_code_not_null",
        "n",
        "NOT NULL code"
      ],
      [
        "interest_group_interest_group_id_not_null",
        "n",
        "NOT NULL interest_group_id"
      ],
      [
        "interest_group_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "interest_group_pkey",
        "p",
        "PRIMARY KEY (interest_group_id)"
      ]
    ],
    "indexes": [
      [
        "interest_group_code_key",
        "CREATE UNIQUE INDEX interest_group_code_key ON public.interest_group USING btree (code)"
      ],
      [
        "interest_group_pkey",
        "CREATE UNIQUE INDEX interest_group_pkey ON public.interest_group USING btree (interest_group_id)"
      ]
    ]
  },
  {
    "table": "item",
    "count": "2",
    "columns": [
      [
        "item_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(150)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "item_type",
        "character varying(30)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "effect_value",
        "numeric(10,2)",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "effective_range",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "usage_limit",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "item_effective_range_check",
        "c",
        "CHECK ((effective_range \u003E= 0))"
      ],
      [
        "item_item_id_not_null",
        "n",
        "NOT NULL item_id"
      ],
      [
        "item_item_type_not_null",
        "n",
        "NOT NULL item_type"
      ],
      [
        "item_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "item_pkey",
        "p",
        "PRIMARY KEY (item_id)"
      ],
      [
        "item_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "item_usage_limit_check",
        "c",
        "CHECK ((usage_limit \u003E= 0))"
      ]
    ],
    "indexes": [
      [
        "item_pkey",
        "CREATE UNIQUE INDEX item_pkey ON public.item USING btree (item_id)"
      ]
    ]
  },
  {
    "table": "journey",
    "count": "82",
    "columns": [
      [
        "journey_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "start_place_id",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "start_latitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "start_longitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "destination_place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "distance",
        "numeric(10,2)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "required_steps",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "recorded_steps",
        "integer",
        "true",
        "0",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "started_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "completed_at",
        "timestamp without time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "journey_check",
        "c",
        "CHECK (((completed_at IS NULL) OR (completed_at \u003E= started_at)))"
      ],
      [
        "journey_destination_place_id_fkey",
        "f",
        "FOREIGN KEY (destination_place_id) REFERENCES place(place_id)"
      ],
      [
        "journey_destination_place_id_not_null",
        "n",
        "NOT NULL destination_place_id"
      ],
      [
        "journey_distance_check",
        "c",
        "CHECK ((distance \u003E= (0)::numeric))"
      ],
      [
        "journey_distance_not_null",
        "n",
        "NOT NULL distance"
      ],
      [
        "journey_journey_id_not_null",
        "n",
        "NOT NULL journey_id"
      ],
      [
        "journey_pkey",
        "p",
        "PRIMARY KEY (journey_id)"
      ],
      [
        "journey_recorded_steps_check",
        "c",
        "CHECK ((recorded_steps \u003E= 0))"
      ],
      [
        "journey_recorded_steps_not_null",
        "n",
        "NOT NULL recorded_steps"
      ],
      [
        "journey_required_steps_check",
        "c",
        "CHECK ((required_steps \u003E 0))"
      ],
      [
        "journey_required_steps_not_null",
        "n",
        "NOT NULL required_steps"
      ],
      [
        "journey_start_latitude_check",
        "c",
        "CHECK (((start_latitude \u003E= (\u0027-90\u0027::integer)::numeric) AND (start_latitude \u003C= (90)::numeric)))"
      ],
      [
        "journey_start_latitude_not_null",
        "n",
        "NOT NULL start_latitude"
      ],
      [
        "journey_start_longitude_check",
        "c",
        "CHECK (((start_longitude \u003E= (\u0027-180\u0027::integer)::numeric) AND (start_longitude \u003C= (180)::numeric)))"
      ],
      [
        "journey_start_longitude_not_null",
        "n",
        "NOT NULL start_longitude"
      ],
      [
        "journey_start_place_id_fkey",
        "f",
        "FOREIGN KEY (start_place_id) REFERENCES place(place_id)"
      ],
      [
        "journey_started_at_not_null",
        "n",
        "NOT NULL started_at"
      ],
      [
        "journey_status_check",
        "c",
        "CHECK (((status)::text = ANY (ARRAY[(\u0027IN_PROGRESS\u0027::character varying)::text, (\u0027COMPLETED\u0027::character varying)::text, (\u0027ABANDONED\u0027::character varying)::text])))"
      ],
      [
        "journey_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "journey_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "journey_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "journey_destination_idx",
        "CREATE INDEX journey_destination_idx ON public.journey USING btree (destination_place_id)"
      ],
      [
        "journey_one_active_per_user",
        "CREATE UNIQUE INDEX journey_one_active_per_user ON public.journey USING btree (user_id) WHERE ((status)::text = \u0027IN_PROGRESS\u0027::text)"
      ],
      [
        "journey_pkey",
        "CREATE UNIQUE INDEX journey_pkey ON public.journey USING btree (journey_id)"
      ],
      [
        "journey_start_place_idx",
        "CREATE INDEX journey_start_place_idx ON public.journey USING btree (start_place_id)"
      ],
      [
        "journey_user_started_idx",
        "CREATE INDEX journey_user_started_idx ON public.journey USING btree (user_id, started_at DESC)"
      ]
    ]
  },
  {
    "table": "media_report",
    "count": "0",
    "columns": [
      [
        "report_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "media_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "reporter_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "reason",
        "character varying(500)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(12)",
        "true",
        "\u0027OPEN\u0027::character varying",
        "",
        "",
        "default"
      ],
      [
        "created_at",
        "timestamp with time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "resolved_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "resolved_by",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "media_report_created_at_not_null",
        "n",
        "NOT NULL created_at"
      ],
      [
        "media_report_media_id_fkey",
        "f",
        "FOREIGN KEY (media_id) REFERENCES community_media(media_id) ON DELETE CASCADE"
      ],
      [
        "media_report_media_id_not_null",
        "n",
        "NOT NULL media_id"
      ],
      [
        "media_report_media_id_reporter_id_key",
        "u",
        "UNIQUE (media_id, reporter_id)"
      ],
      [
        "media_report_pkey",
        "p",
        "PRIMARY KEY (report_id)"
      ],
      [
        "media_report_reason_check",
        "c",
        "CHECK ((length(TRIM(BOTH FROM reason)) \u003E 0))"
      ],
      [
        "media_report_reason_not_null",
        "n",
        "NOT NULL reason"
      ],
      [
        "media_report_report_id_not_null",
        "n",
        "NOT NULL report_id"
      ],
      [
        "media_report_reporter_id_fkey",
        "f",
        "FOREIGN KEY (reporter_id) REFERENCES app_user(user_id)"
      ],
      [
        "media_report_reporter_id_not_null",
        "n",
        "NOT NULL reporter_id"
      ],
      [
        "media_report_resolved_by_fkey",
        "f",
        "FOREIGN KEY (resolved_by) REFERENCES app_user(user_id)"
      ],
      [
        "media_report_status_check",
        "c",
        "CHECK (((status)::text = ANY ((ARRAY[\u0027OPEN\u0027::character varying, \u0027RESOLVED\u0027::character varying, \u0027ACCEPTED\u0027::character varying, \u0027DISMISSED\u0027::character varying])::text[])))"
      ],
      [
        "media_report_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "media_report_media_id_reporter_id_key",
        "CREATE UNIQUE INDEX media_report_media_id_reporter_id_key ON public.media_report USING btree (media_id, reporter_id)"
      ],
      [
        "media_report_open_idx",
        "CREATE INDEX media_report_open_idx ON public.media_report USING btree (media_id, status)"
      ],
      [
        "media_report_pkey",
        "CREATE UNIQUE INDEX media_report_pkey ON public.media_report USING btree (report_id)"
      ]
    ]
  },
  {
    "table": "notification",
    "count": "317",
    "columns": [
      [
        "notification_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "notification_type",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "title",
        "character varying(200)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "content",
        "text",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "read_status",
        "boolean",
        "true",
        "false",
        "",
        "",
        "NULL"
      ],
      [
        "created_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "hidden_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "notification_content_not_null",
        "n",
        "NOT NULL content"
      ],
      [
        "notification_created_at_not_null",
        "n",
        "NOT NULL created_at"
      ],
      [
        "notification_notification_id_not_null",
        "n",
        "NOT NULL notification_id"
      ],
      [
        "notification_notification_type_not_null",
        "n",
        "NOT NULL notification_type"
      ],
      [
        "notification_pkey",
        "p",
        "PRIMARY KEY (notification_id)"
      ],
      [
        "notification_read_status_not_null",
        "n",
        "NOT NULL read_status"
      ],
      [
        "notification_title_not_null",
        "n",
        "NOT NULL title"
      ],
      [
        "notification_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "notification_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "notification_pkey",
        "CREATE UNIQUE INDEX notification_pkey ON public.notification USING btree (notification_id)"
      ],
      [
        "notification_user_created_idx",
        "CREATE INDEX notification_user_created_idx ON public.notification USING btree (user_id, created_at DESC)"
      ]
    ]
  },
  {
    "table": "place",
    "count": "312",
    "columns": [
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "region_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "place_type_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(200)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "latitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "longitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "introduction",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "highlights",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "rating",
        "numeric(3,2)",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "rating_count",
        "integer",
        "true",
        "0",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "feature_source",
        "character varying(10)",
        "true",
        "\u0027CUSTOM\u0027::character varying",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "place_feature_source_check",
        "c",
        "CHECK (((feature_source)::text = ANY ((ARRAY[\u0027TYPE\u0027::character varying, \u0027CUSTOM\u0027::character varying])::text[])))"
      ],
      [
        "place_feature_source_not_null",
        "n",
        "NOT NULL feature_source"
      ],
      [
        "place_latitude_check",
        "c",
        "CHECK (((latitude \u003E= (\u0027-90\u0027::integer)::numeric) AND (latitude \u003C= (90)::numeric)))"
      ],
      [
        "place_latitude_not_null",
        "n",
        "NOT NULL latitude"
      ],
      [
        "place_longitude_check",
        "c",
        "CHECK (((longitude \u003E= (\u0027-180\u0027::integer)::numeric) AND (longitude \u003C= (180)::numeric)))"
      ],
      [
        "place_longitude_not_null",
        "n",
        "NOT NULL longitude"
      ],
      [
        "place_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "place_pkey",
        "p",
        "PRIMARY KEY (place_id)"
      ],
      [
        "place_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "place_place_type_id_fkey",
        "f",
        "FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id)"
      ],
      [
        "place_place_type_id_not_null",
        "n",
        "NOT NULL place_type_id"
      ],
      [
        "place_rating_check",
        "c",
        "CHECK (((rating \u003E= (0)::numeric) AND (rating \u003C= (5)::numeric)))"
      ],
      [
        "place_rating_count_check",
        "c",
        "CHECK ((rating_count \u003E= 0))"
      ],
      [
        "place_rating_count_not_null",
        "n",
        "NOT NULL rating_count"
      ],
      [
        "place_region_id_fkey",
        "f",
        "FOREIGN KEY (region_id) REFERENCES region(region_id)"
      ],
      [
        "place_region_id_not_null",
        "n",
        "NOT NULL region_id"
      ],
      [
        "place_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "place_geography_idx",
        "CREATE INDEX place_geography_idx ON public.place USING gist (((st_setsrid(st_makepoint((longitude)::double precision, (latitude)::double precision), 4326))::geography))"
      ],
      [
        "place_pkey",
        "CREATE UNIQUE INDEX place_pkey ON public.place USING btree (place_id)"
      ],
      [
        "place_region_idx",
        "CREATE INDEX place_region_idx ON public.place USING btree (region_id)"
      ],
      [
        "place_type_idx",
        "CREATE INDEX place_type_idx ON public.place USING btree (place_type_id)"
      ]
    ]
  },
  {
    "table": "place_feature",
    "count": "312",
    "columns": [
      [
        "place_feature_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "recommendation_config_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "cluster_number",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "updated_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "place_feature_cluster_number_check",
        "c",
        "CHECK ((cluster_number \u003E= 0))"
      ],
      [
        "place_feature_pkey",
        "p",
        "PRIMARY KEY (place_feature_id)"
      ],
      [
        "place_feature_place_feature_id_not_null",
        "n",
        "NOT NULL place_feature_id"
      ],
      [
        "place_feature_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "place_feature_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "place_feature_place_id_recommendation_config_id_key",
        "u",
        "UNIQUE (place_id, recommendation_config_id)"
      ],
      [
        "place_feature_recommendation_config_id_fkey",
        "f",
        "FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id)"
      ],
      [
        "place_feature_recommendation_config_id_not_null",
        "n",
        "NOT NULL recommendation_config_id"
      ],
      [
        "place_feature_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ]
    ],
    "indexes": [
      [
        "place_feature_config_idx",
        "CREATE INDEX place_feature_config_idx ON public.place_feature USING btree (recommendation_config_id)"
      ],
      [
        "place_feature_pkey",
        "CREATE UNIQUE INDEX place_feature_pkey ON public.place_feature USING btree (place_feature_id)"
      ],
      [
        "place_feature_place_id_recommendation_config_id_key",
        "CREATE UNIQUE INDEX place_feature_place_id_recommendation_config_id_key ON public.place_feature USING btree (place_id, recommendation_config_id)"
      ]
    ]
  },
  {
    "table": "place_feature_value",
    "count": "10920",
    "columns": [
      [
        "place_feature_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "criterion_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "feature_value",
        "double precision",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "place_feature_value_criterion_id_fkey",
        "f",
        "FOREIGN KEY (criterion_id) REFERENCES recommendation_criterion(criterion_id)"
      ],
      [
        "place_feature_value_criterion_id_not_null",
        "n",
        "NOT NULL criterion_id"
      ],
      [
        "place_feature_value_feature_value_not_null",
        "n",
        "NOT NULL feature_value"
      ],
      [
        "place_feature_value_pkey",
        "p",
        "PRIMARY KEY (place_feature_id, criterion_id)"
      ],
      [
        "place_feature_value_place_feature_id_fkey",
        "f",
        "FOREIGN KEY (place_feature_id) REFERENCES place_feature(place_feature_id)"
      ],
      [
        "place_feature_value_place_feature_id_not_null",
        "n",
        "NOT NULL place_feature_id"
      ]
    ],
    "indexes": [
      [
        "place_feature_value_criterion_idx",
        "CREATE INDEX place_feature_value_criterion_idx ON public.place_feature_value USING btree (criterion_id)"
      ],
      [
        "place_feature_value_pkey",
        "CREATE UNIQUE INDEX place_feature_value_pkey ON public.place_feature_value USING btree (place_feature_id, criterion_id)"
      ]
    ]
  },
  {
    "table": "place_image",
    "count": "68",
    "columns": [
      [
        "place_image_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "image_key",
        "text",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "place_image_image_key_not_null",
        "n",
        "NOT NULL image_key"
      ],
      [
        "place_image_pkey",
        "p",
        "PRIMARY KEY (place_image_id)"
      ],
      [
        "place_image_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "place_image_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "place_image_place_image_id_not_null",
        "n",
        "NOT NULL place_image_id"
      ]
    ],
    "indexes": [
      [
        "place_image_pkey",
        "CREATE UNIQUE INDEX place_image_pkey ON public.place_image USING btree (place_image_id)"
      ],
      [
        "place_image_place_idx",
        "CREATE INDEX place_image_place_idx ON public.place_image USING btree (place_id)"
      ]
    ]
  },
  {
    "table": "place_type",
    "count": "22",
    "columns": [
      [
        "place_type_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(100)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "place_type_name_key",
        "u",
        "UNIQUE (name)"
      ],
      [
        "place_type_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "place_type_pkey",
        "p",
        "PRIMARY KEY (place_type_id)"
      ],
      [
        "place_type_place_type_id_not_null",
        "n",
        "NOT NULL place_type_id"
      ]
    ],
    "indexes": [
      [
        "place_type_name_key",
        "CREATE UNIQUE INDEX place_type_name_key ON public.place_type USING btree (name)"
      ],
      [
        "place_type_pkey",
        "CREATE UNIQUE INDEX place_type_pkey ON public.place_type USING btree (place_type_id)"
      ]
    ]
  },
  {
    "table": "place_type_feature",
    "count": "0",
    "columns": [
      [
        "place_type_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "code",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "place_type_feature_code_not_null",
        "n",
        "NOT NULL code"
      ],
      [
        "place_type_feature_pkey",
        "p",
        "PRIMARY KEY (place_type_id, code)"
      ],
      [
        "place_type_feature_place_type_id_fkey",
        "f",
        "FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id) ON DELETE CASCADE"
      ],
      [
        "place_type_feature_place_type_id_not_null",
        "n",
        "NOT NULL place_type_id"
      ]
    ],
    "indexes": [
      [
        "place_type_feature_pkey",
        "CREATE UNIQUE INDEX place_type_feature_pkey ON public.place_type_feature USING btree (place_type_id, code)"
      ]
    ]
  },
  {
    "table": "quest",
    "count": "43",
    "columns": [
      [
        "quest_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "reward_item_id",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(200)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "quest_type",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "condition_type",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "target_value",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "cycle_type",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "reward_quantity",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "reward_points",
        "integer",
        "true",
        "20",
        "",
        "",
        "NULL"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "quest_check",
        "c",
        "CHECK (((reward_item_id IS NULL) = (reward_quantity IS NULL)))"
      ],
      [
        "quest_condition_type_not_null",
        "n",
        "NOT NULL condition_type"
      ],
      [
        "quest_cycle_type_not_null",
        "n",
        "NOT NULL cycle_type"
      ],
      [
        "quest_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "quest_pkey",
        "p",
        "PRIMARY KEY (quest_id)"
      ],
      [
        "quest_quest_id_not_null",
        "n",
        "NOT NULL quest_id"
      ],
      [
        "quest_quest_type_not_null",
        "n",
        "NOT NULL quest_type"
      ],
      [
        "quest_reward_item_id_fkey",
        "f",
        "FOREIGN KEY (reward_item_id) REFERENCES item(item_id)"
      ],
      [
        "quest_reward_points_not_null",
        "n",
        "NOT NULL reward_points"
      ],
      [
        "quest_reward_points_range",
        "c",
        "CHECK (((reward_points \u003E= 0) AND (reward_points \u003C= 100000)))"
      ],
      [
        "quest_reward_quantity_check",
        "c",
        "CHECK ((reward_quantity \u003E 0))"
      ],
      [
        "quest_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "quest_target_value_check",
        "c",
        "CHECK ((target_value \u003E 0))"
      ],
      [
        "quest_target_value_not_null",
        "n",
        "NOT NULL target_value"
      ]
    ],
    "indexes": [
      [
        "quest_pkey",
        "CREATE UNIQUE INDEX quest_pkey ON public.quest USING btree (quest_id)"
      ],
      [
        "quest_reward_item_idx",
        "CREATE INDEX quest_reward_item_idx ON public.quest USING btree (reward_item_id)"
      ]
    ]
  },
  {
    "table": "quest_progress",
    "count": "333",
    "columns": [
      [
        "quest_progress_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "quest_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "cycle_key",
        "character varying(30)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "progress_value",
        "integer",
        "true",
        "0",
        "",
        "",
        "NULL"
      ],
      [
        "completion_status",
        "boolean",
        "true",
        "false",
        "",
        "",
        "NULL"
      ],
      [
        "completed_at",
        "timestamp without time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "reward_status",
        "boolean",
        "true",
        "false",
        "",
        "",
        "NULL"
      ],
      [
        "awarded_points",
        "integer",
        "true",
        "0",
        "",
        "",
        "NULL"
      ],
      [
        "points_settled",
        "boolean",
        "true",
        "false",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "quest_progress_awarded_points_not_null",
        "n",
        "NOT NULL awarded_points"
      ],
      [
        "quest_progress_check",
        "c",
        "CHECK (((completed_at IS NULL) OR completion_status))"
      ],
      [
        "quest_progress_completion_status_not_null",
        "n",
        "NOT NULL completion_status"
      ],
      [
        "quest_progress_cycle_key_not_null",
        "n",
        "NOT NULL cycle_key"
      ],
      [
        "quest_progress_pkey",
        "p",
        "PRIMARY KEY (quest_progress_id)"
      ],
      [
        "quest_progress_points_settled_not_null",
        "n",
        "NOT NULL points_settled"
      ],
      [
        "quest_progress_progress_value_check",
        "c",
        "CHECK ((progress_value \u003E= 0))"
      ],
      [
        "quest_progress_progress_value_not_null",
        "n",
        "NOT NULL progress_value"
      ],
      [
        "quest_progress_quest_id_fkey",
        "f",
        "FOREIGN KEY (quest_id) REFERENCES quest(quest_id)"
      ],
      [
        "quest_progress_quest_id_not_null",
        "n",
        "NOT NULL quest_id"
      ],
      [
        "quest_progress_quest_progress_id_not_null",
        "n",
        "NOT NULL quest_progress_id"
      ],
      [
        "quest_progress_reward_status_not_null",
        "n",
        "NOT NULL reward_status"
      ],
      [
        "quest_progress_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "quest_progress_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "quest_progress_user_id_quest_id_cycle_key_key",
        "u",
        "UNIQUE (user_id, quest_id, cycle_key)"
      ]
    ],
    "indexes": [
      [
        "quest_progress_pkey",
        "CREATE UNIQUE INDEX quest_progress_pkey ON public.quest_progress USING btree (quest_progress_id)"
      ],
      [
        "quest_progress_quest_idx",
        "CREATE INDEX quest_progress_quest_idx ON public.quest_progress USING btree (quest_id)"
      ],
      [
        "quest_progress_user_id_quest_id_cycle_key_key",
        "CREATE UNIQUE INDEX quest_progress_user_id_quest_id_cycle_key_key ON public.quest_progress USING btree (user_id, quest_id, cycle_key)"
      ]
    ]
  },
  {
    "table": "recommendation_config",
    "count": "1",
    "columns": [
      [
        "recommendation_config_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "version",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "distance_limit",
        "numeric(10,2)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "cluster_count",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "updated_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "settings",
        "jsonb",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "recommendation_config_cluster_count_check",
        "c",
        "CHECK ((cluster_count \u003E 0))"
      ],
      [
        "recommendation_config_cluster_count_not_null",
        "n",
        "NOT NULL cluster_count"
      ],
      [
        "recommendation_config_distance_limit_check",
        "c",
        "CHECK ((distance_limit \u003E (0)::numeric))"
      ],
      [
        "recommendation_config_distance_limit_not_null",
        "n",
        "NOT NULL distance_limit"
      ],
      [
        "recommendation_config_pkey",
        "p",
        "PRIMARY KEY (recommendation_config_id)"
      ],
      [
        "recommendation_config_recommendation_config_id_not_null",
        "n",
        "NOT NULL recommendation_config_id"
      ],
      [
        "recommendation_config_settings_not_null",
        "n",
        "NOT NULL settings"
      ],
      [
        "recommendation_config_status_not_null",
        "n",
        "NOT NULL status"
      ],
      [
        "recommendation_config_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ],
      [
        "recommendation_config_version_key",
        "u",
        "UNIQUE (version)"
      ],
      [
        "recommendation_config_version_not_null",
        "n",
        "NOT NULL version"
      ]
    ],
    "indexes": [
      [
        "one_active_recommendation_config",
        "CREATE UNIQUE INDEX one_active_recommendation_config ON public.recommendation_config USING btree (status) WHERE ((status)::text = \u0027ACTIVE\u0027::text)"
      ],
      [
        "recommendation_config_pkey",
        "CREATE UNIQUE INDEX recommendation_config_pkey ON public.recommendation_config USING btree (recommendation_config_id)"
      ],
      [
        "recommendation_config_version_key",
        "CREATE UNIQUE INDEX recommendation_config_version_key ON public.recommendation_config USING btree (version)"
      ]
    ]
  },
  {
    "table": "recommendation_criterion",
    "count": "40",
    "columns": [
      [
        "criterion_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "code",
        "character varying(50)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "name",
        "character varying(100)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "recommendation_criterion_code_key",
        "u",
        "UNIQUE (code)"
      ],
      [
        "recommendation_criterion_code_not_null",
        "n",
        "NOT NULL code"
      ],
      [
        "recommendation_criterion_criterion_id_not_null",
        "n",
        "NOT NULL criterion_id"
      ],
      [
        "recommendation_criterion_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "recommendation_criterion_pkey",
        "p",
        "PRIMARY KEY (criterion_id)"
      ],
      [
        "recommendation_criterion_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "recommendation_criterion_code_key",
        "CREATE UNIQUE INDEX recommendation_criterion_code_key ON public.recommendation_criterion USING btree (code)"
      ],
      [
        "recommendation_criterion_pkey",
        "CREATE UNIQUE INDEX recommendation_criterion_pkey ON public.recommendation_criterion USING btree (criterion_id)"
      ]
    ]
  },
  {
    "table": "recommendation_rating_snapshot",
    "count": "312",
    "columns": [
      [
        "recommendation_config_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "rating",
        "numeric(3,2)",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "rating_count",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "recommendation_rating_snapsho_recommendation_config_id_not_null",
        "n",
        "NOT NULL recommendation_config_id"
      ],
      [
        "recommendation_rating_snapshot_pkey",
        "p",
        "PRIMARY KEY (recommendation_config_id, place_id)"
      ],
      [
        "recommendation_rating_snapshot_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "recommendation_rating_snapshot_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "recommendation_rating_snapshot_rating_check",
        "c",
        "CHECK (((rating IS NULL) OR ((rating \u003E= (0)::numeric) AND (rating \u003C= (5)::numeric))))"
      ],
      [
        "recommendation_rating_snapshot_rating_count_check",
        "c",
        "CHECK (((rating_count IS NULL) OR (rating_count \u003E= 0)))"
      ],
      [
        "recommendation_rating_snapshot_recommendation_config_id_fkey",
        "f",
        "FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id)"
      ]
    ],
    "indexes": [
      [
        "recommendation_rating_snapshot_pkey",
        "CREATE UNIQUE INDEX recommendation_rating_snapshot_pkey ON public.recommendation_rating_snapshot USING btree (recommendation_config_id, place_id)"
      ]
    ]
  },
  {
    "table": "recommendation_weight",
    "count": "5",
    "columns": [
      [
        "recommendation_config_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "criterion_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "weight_value",
        "numeric(6,4)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "recommendation_weight_criterion_id_fkey",
        "f",
        "FOREIGN KEY (criterion_id) REFERENCES recommendation_criterion(criterion_id)"
      ],
      [
        "recommendation_weight_criterion_id_not_null",
        "n",
        "NOT NULL criterion_id"
      ],
      [
        "recommendation_weight_pkey",
        "p",
        "PRIMARY KEY (recommendation_config_id, criterion_id)"
      ],
      [
        "recommendation_weight_recommendation_config_id_fkey",
        "f",
        "FOREIGN KEY (recommendation_config_id) REFERENCES recommendation_config(recommendation_config_id)"
      ],
      [
        "recommendation_weight_recommendation_config_id_not_null",
        "n",
        "NOT NULL recommendation_config_id"
      ],
      [
        "recommendation_weight_weight_value_check",
        "c",
        "CHECK ((weight_value \u003E= (0)::numeric))"
      ],
      [
        "recommendation_weight_weight_value_not_null",
        "n",
        "NOT NULL weight_value"
      ]
    ],
    "indexes": [
      [
        "recommendation_weight_criterion_idx",
        "CREATE INDEX recommendation_weight_criterion_idx ON public.recommendation_weight USING btree (criterion_id)"
      ],
      [
        "recommendation_weight_pkey",
        "CREATE UNIQUE INDEX recommendation_weight_pkey ON public.recommendation_weight USING btree (recommendation_config_id, criterion_id)"
      ]
    ]
  },
  {
    "table": "region",
    "count": "63",
    "columns": [
      [
        "region_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "name",
        "character varying(150)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "description",
        "text",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "status",
        "character varying(20)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "region_name_not_null",
        "n",
        "NOT NULL name"
      ],
      [
        "region_pkey",
        "p",
        "PRIMARY KEY (region_id)"
      ],
      [
        "region_region_id_not_null",
        "n",
        "NOT NULL region_id"
      ],
      [
        "region_status_not_null",
        "n",
        "NOT NULL status"
      ]
    ],
    "indexes": [
      [
        "region_pkey",
        "CREATE UNIQUE INDEX region_pkey ON public.region USING btree (region_id)"
      ]
    ]
  },
  {
    "table": "spatial_ref_sys",
    "count": "8500",
    "columns": [
      [
        "srid",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "auth_name",
        "character varying(256)",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "auth_srid",
        "integer",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "srtext",
        "character varying(2048)",
        "false",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "proj4text",
        "character varying(2048)",
        "false",
        "NULL",
        "",
        "",
        "default"
      ]
    ],
    "constraints": [
      [
        "spatial_ref_sys_pkey",
        "p",
        "PRIMARY KEY (srid)"
      ],
      [
        "spatial_ref_sys_srid_check",
        "c",
        "CHECK (((srid \u003E 0) AND (srid \u003C= 998999)))"
      ],
      [
        "spatial_ref_sys_srid_not_null",
        "n",
        "NOT NULL srid"
      ]
    ],
    "indexes": [
      [
        "spatial_ref_sys_pkey",
        "CREATE UNIQUE INDEX spatial_ref_sys_pkey ON public.spatial_ref_sys USING btree (srid)"
      ]
    ]
  },
  {
    "table": "step_data",
    "count": "72",
    "columns": [
      [
        "step_data_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "source_data_id",
        "character varying(255)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "step_count",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "start_time",
        "timestamp without time zone",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "end_time",
        "timestamp without time zone",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "synced_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "step_data_check",
        "c",
        "CHECK ((end_time \u003E= start_time))"
      ],
      [
        "step_data_end_time_not_null",
        "n",
        "NOT NULL end_time"
      ],
      [
        "step_data_pkey",
        "p",
        "PRIMARY KEY (step_data_id)"
      ],
      [
        "step_data_source_data_id_not_null",
        "n",
        "NOT NULL source_data_id"
      ],
      [
        "step_data_start_time_not_null",
        "n",
        "NOT NULL start_time"
      ],
      [
        "step_data_step_count_check",
        "c",
        "CHECK ((step_count \u003E= 0))"
      ],
      [
        "step_data_step_count_not_null",
        "n",
        "NOT NULL step_count"
      ],
      [
        "step_data_step_data_id_not_null",
        "n",
        "NOT NULL step_data_id"
      ],
      [
        "step_data_synced_at_not_null",
        "n",
        "NOT NULL synced_at"
      ],
      [
        "step_data_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "step_data_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "step_data_user_id_source_data_id_key",
        "u",
        "UNIQUE (user_id, source_data_id)"
      ]
    ],
    "indexes": [
      [
        "step_data_pkey",
        "CREATE UNIQUE INDEX step_data_pkey ON public.step_data USING btree (step_data_id)"
      ],
      [
        "step_data_user_id_source_data_id_key",
        "CREATE UNIQUE INDEX step_data_user_id_source_data_id_key ON public.step_data USING btree (user_id, source_data_id)"
      ],
      [
        "step_data_user_start_idx",
        "CREATE INDEX step_data_user_start_idx ON public.step_data USING btree (user_id, start_time DESC)"
      ]
    ]
  },
  {
    "table": "user_card",
    "count": "42",
    "columns": [
      [
        "user_card_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "card_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "received_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ],
      [
        "seen_at",
        "timestamp with time zone",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_card_card_id_fkey",
        "f",
        "FOREIGN KEY (card_id) REFERENCES card(card_id)"
      ],
      [
        "user_card_card_id_not_null",
        "n",
        "NOT NULL card_id"
      ],
      [
        "user_card_pkey",
        "p",
        "PRIMARY KEY (user_card_id)"
      ],
      [
        "user_card_received_at_not_null",
        "n",
        "NOT NULL received_at"
      ],
      [
        "user_card_user_card_id_not_null",
        "n",
        "NOT NULL user_card_id"
      ],
      [
        "user_card_user_id_card_id_key",
        "u",
        "UNIQUE (user_id, card_id)"
      ],
      [
        "user_card_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "user_card_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ]
    ],
    "indexes": [
      [
        "user_card_card_idx",
        "CREATE INDEX user_card_card_idx ON public.user_card USING btree (card_id)"
      ],
      [
        "user_card_pkey",
        "CREATE UNIQUE INDEX user_card_pkey ON public.user_card USING btree (user_card_id)"
      ],
      [
        "user_card_user_id_card_id_key",
        "CREATE UNIQUE INDEX user_card_user_id_card_id_key ON public.user_card USING btree (user_id, card_id)"
      ]
    ]
  },
  {
    "table": "user_discovered_place",
    "count": "74",
    "columns": [
      [
        "user_discovered_place_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "place_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "discovered_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_discovered_place_discovered_at_not_null",
        "n",
        "NOT NULL discovered_at"
      ],
      [
        "user_discovered_place_pkey",
        "p",
        "PRIMARY KEY (user_discovered_place_id)"
      ],
      [
        "user_discovered_place_place_id_fkey",
        "f",
        "FOREIGN KEY (place_id) REFERENCES place(place_id)"
      ],
      [
        "user_discovered_place_place_id_not_null",
        "n",
        "NOT NULL place_id"
      ],
      [
        "user_discovered_place_user_discovered_place_id_not_null",
        "n",
        "NOT NULL user_discovered_place_id"
      ],
      [
        "user_discovered_place_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "user_discovered_place_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "user_discovered_place_user_id_place_id_key",
        "u",
        "UNIQUE (user_id, place_id)"
      ]
    ],
    "indexes": [
      [
        "user_discovered_place_pkey",
        "CREATE UNIQUE INDEX user_discovered_place_pkey ON public.user_discovered_place USING btree (user_discovered_place_id)"
      ],
      [
        "user_discovered_place_place_idx",
        "CREATE INDEX user_discovered_place_place_idx ON public.user_discovered_place USING btree (place_id)"
      ],
      [
        "user_discovered_place_user_id_place_id_key",
        "CREATE UNIQUE INDEX user_discovered_place_user_id_place_id_key ON public.user_discovered_place USING btree (user_id, place_id)"
      ]
    ]
  },
  {
    "table": "user_item",
    "count": "43",
    "columns": [
      [
        "user_item_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "item_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "quantity",
        "integer",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "updated_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_item_item_id_fkey",
        "f",
        "FOREIGN KEY (item_id) REFERENCES item(item_id)"
      ],
      [
        "user_item_item_id_not_null",
        "n",
        "NOT NULL item_id"
      ],
      [
        "user_item_pkey",
        "p",
        "PRIMARY KEY (user_item_id)"
      ],
      [
        "user_item_quantity_check",
        "c",
        "CHECK ((quantity \u003E= 0))"
      ],
      [
        "user_item_quantity_not_null",
        "n",
        "NOT NULL quantity"
      ],
      [
        "user_item_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ],
      [
        "user_item_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "user_item_user_id_item_id_key",
        "u",
        "UNIQUE (user_id, item_id)"
      ],
      [
        "user_item_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "user_item_user_item_id_not_null",
        "n",
        "NOT NULL user_item_id"
      ]
    ],
    "indexes": [
      [
        "user_item_item_idx",
        "CREATE INDEX user_item_item_idx ON public.user_item USING btree (item_id)"
      ],
      [
        "user_item_pkey",
        "CREATE UNIQUE INDEX user_item_pkey ON public.user_item USING btree (user_item_id)"
      ],
      [
        "user_item_user_id_item_id_key",
        "CREATE UNIQUE INDEX user_item_user_id_item_id_key ON public.user_item USING btree (user_id, item_id)"
      ]
    ]
  },
  {
    "table": "user_preference",
    "count": "69",
    "columns": [
      [
        "user_preference_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "activity_level",
        "character varying(30)",
        "true",
        "NULL",
        "",
        "",
        "default"
      ],
      [
        "updated_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_preference_activity_level_not_null",
        "n",
        "NOT NULL activity_level"
      ],
      [
        "user_preference_pkey",
        "p",
        "PRIMARY KEY (user_preference_id)"
      ],
      [
        "user_preference_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ],
      [
        "user_preference_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "user_preference_user_id_key",
        "u",
        "UNIQUE (user_id)"
      ],
      [
        "user_preference_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "user_preference_user_preference_id_not_null",
        "n",
        "NOT NULL user_preference_id"
      ]
    ],
    "indexes": [
      [
        "user_preference_pkey",
        "CREATE UNIQUE INDEX user_preference_pkey ON public.user_preference USING btree (user_preference_id)"
      ],
      [
        "user_preference_user_id_key",
        "CREATE UNIQUE INDEX user_preference_user_id_key ON public.user_preference USING btree (user_id)"
      ]
    ]
  },
  {
    "table": "user_preference_interest",
    "count": "96",
    "columns": [
      [
        "user_preference_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "interest_group_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_preference_interest_interest_group_id_fkey",
        "f",
        "FOREIGN KEY (interest_group_id) REFERENCES interest_group(interest_group_id)"
      ],
      [
        "user_preference_interest_interest_group_id_not_null",
        "n",
        "NOT NULL interest_group_id"
      ],
      [
        "user_preference_interest_pkey",
        "p",
        "PRIMARY KEY (user_preference_id, interest_group_id)"
      ],
      [
        "user_preference_interest_user_preference_id_fkey",
        "f",
        "FOREIGN KEY (user_preference_id) REFERENCES user_preference(user_preference_id) ON DELETE CASCADE"
      ],
      [
        "user_preference_interest_user_preference_id_not_null",
        "n",
        "NOT NULL user_preference_id"
      ]
    ],
    "indexes": [
      [
        "user_preference_interest_pkey",
        "CREATE UNIQUE INDEX user_preference_interest_pkey ON public.user_preference_interest USING btree (user_preference_id, interest_group_id)"
      ]
    ]
  },
  {
    "table": "user_preference_place_type",
    "count": "7",
    "columns": [
      [
        "user_preference_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "place_type_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "user_preference_place_type_pkey",
        "p",
        "PRIMARY KEY (user_preference_id, place_type_id)"
      ],
      [
        "user_preference_place_type_place_type_id_fkey",
        "f",
        "FOREIGN KEY (place_type_id) REFERENCES place_type(place_type_id)"
      ],
      [
        "user_preference_place_type_place_type_id_not_null",
        "n",
        "NOT NULL place_type_id"
      ],
      [
        "user_preference_place_type_user_preference_id_fkey",
        "f",
        "FOREIGN KEY (user_preference_id) REFERENCES user_preference(user_preference_id)"
      ],
      [
        "user_preference_place_type_user_preference_id_not_null",
        "n",
        "NOT NULL user_preference_id"
      ]
    ],
    "indexes": [
      [
        "user_preference_place_type_pkey",
        "CREATE UNIQUE INDEX user_preference_place_type_pkey ON public.user_preference_place_type USING btree (user_preference_id, place_type_id)"
      ]
    ]
  },
  {
    "table": "virtual_location",
    "count": "69",
    "columns": [
      [
        "virtual_location_id",
        "bigint",
        "true",
        "NULL",
        "d",
        "",
        "NULL"
      ],
      [
        "user_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "region_id",
        "bigint",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "nearest_place_id",
        "bigint",
        "false",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "latitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "longitude",
        "numeric(9,6)",
        "true",
        "NULL",
        "",
        "",
        "NULL"
      ],
      [
        "updated_at",
        "timestamp without time zone",
        "true",
        "CURRENT_TIMESTAMP",
        "",
        "",
        "NULL"
      ]
    ],
    "constraints": [
      [
        "virtual_location_latitude_check",
        "c",
        "CHECK (((latitude \u003E= (\u0027-90\u0027::integer)::numeric) AND (latitude \u003C= (90)::numeric)))"
      ],
      [
        "virtual_location_latitude_not_null",
        "n",
        "NOT NULL latitude"
      ],
      [
        "virtual_location_longitude_check",
        "c",
        "CHECK (((longitude \u003E= (\u0027-180\u0027::integer)::numeric) AND (longitude \u003C= (180)::numeric)))"
      ],
      [
        "virtual_location_longitude_not_null",
        "n",
        "NOT NULL longitude"
      ],
      [
        "virtual_location_nearest_place_id_fkey",
        "f",
        "FOREIGN KEY (nearest_place_id) REFERENCES place(place_id)"
      ],
      [
        "virtual_location_pkey",
        "p",
        "PRIMARY KEY (virtual_location_id)"
      ],
      [
        "virtual_location_region_id_fkey",
        "f",
        "FOREIGN KEY (region_id) REFERENCES region(region_id)"
      ],
      [
        "virtual_location_region_id_not_null",
        "n",
        "NOT NULL region_id"
      ],
      [
        "virtual_location_updated_at_not_null",
        "n",
        "NOT NULL updated_at"
      ],
      [
        "virtual_location_user_id_fkey",
        "f",
        "FOREIGN KEY (user_id) REFERENCES app_user(user_id)"
      ],
      [
        "virtual_location_user_id_key",
        "u",
        "UNIQUE (user_id)"
      ],
      [
        "virtual_location_user_id_not_null",
        "n",
        "NOT NULL user_id"
      ],
      [
        "virtual_location_virtual_location_id_not_null",
        "n",
        "NOT NULL virtual_location_id"
      ]
    ],
    "indexes": [
      [
        "virtual_location_geography_idx",
        "CREATE INDEX virtual_location_geography_idx ON public.virtual_location USING gist (((st_setsrid(st_makepoint((longitude)::double precision, (latitude)::double precision), 4326))::geography))"
      ],
      [
        "virtual_location_nearest_place_idx",
        "CREATE INDEX virtual_location_nearest_place_idx ON public.virtual_location USING btree (nearest_place_id)"
      ],
      [
        "virtual_location_pkey",
        "CREATE UNIQUE INDEX virtual_location_pkey ON public.virtual_location USING btree (virtual_location_id)"
      ],
      [
        "virtual_location_region_idx",
        "CREATE INDEX virtual_location_region_idx ON public.virtual_location USING btree (region_id)"
      ],
      [
        "virtual_location_user_id_key",
        "CREATE UNIQUE INDEX virtual_location_user_id_key ON public.virtual_location USING btree (user_id)"
      ]
    ]
  }
]
```
