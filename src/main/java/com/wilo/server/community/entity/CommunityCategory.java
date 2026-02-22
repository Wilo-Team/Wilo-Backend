package com.wilo.server.community.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CommunityCategory {
    TREE_SHADE("나무그늘"),
    SUNNY_PLACE("볕드는 곳"),
    HELP_BRANCH("도움가지"),
    SUPPORT_ROOT("버팀뿌리");

    private final String displayName;
}
