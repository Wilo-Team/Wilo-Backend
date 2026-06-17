CREATE INDEX idx_chat_messages_session_id ON chat_messages (session_id, id);
CREATE INDEX idx_chat_sessions_user_status_last ON chat_sessions (user_id, status, last_message_at, id);
CREATE INDEX idx_chat_sessions_guest_status_last ON chat_sessions (guest_id, status, last_message_at, id);

CREATE INDEX idx_posts_created_id ON community_posts (created_at, id);
CREATE INDEX idx_posts_category_created_id ON community_posts (category, created_at, id);
CREATE INDEX idx_posts_like_created_id ON community_posts (like_count, created_at, id);
CREATE INDEX idx_posts_user_created_id ON community_posts (user_id, created_at, id);
CREATE INDEX idx_comments_post_created_id ON community_comments (post_id, created_at, id);
CREATE INDEX idx_comments_user_created_id ON community_comments (user_id, created_at, id);
CREATE INDEX idx_post_images_post_order ON community_post_images (post_id, sort_order);
