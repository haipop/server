/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.imhandler;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.GroupNotificationBinaryContent;
import cn.wildfirechat.proto.WFCMessage;
import io.moquette.spi.impl.Qos1PublishHandler;
import io.netty.buffer.ByteBuf;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_JoinType;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Mute;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Name;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Portrait;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_PrivateChat;
import static cn.wildfirechat.proto.ProtoConstants.ModifyGroupInfoType.Modify_Group_Searchable;
import static win.liyufan.im.IMTopic.ModifyGroupInfoTopic;

@Handler(value = ModifyGroupInfoTopic)
public class ModifyGroupInfoHandler extends GroupHandler<WFCMessage.ModifyGroupInfoRequest> {
    @Override
    public ErrorCode action(ByteBuf ackPayload, String clientID, String fromUser, boolean isAdmin, WFCMessage.ModifyGroupInfoRequest request, Qos1PublishHandler.IMCallback callback) {
        ErrorCode errorCode = m_messagesStore.modifyGroupInfo(fromUser, request.getGroupId(), request.getType(), request.getValue(), isAdmin);
        if (errorCode == ERROR_CODE_SUCCESS) {
            if (request.hasNotifyContent() && request.getNotifyContent().getType() > 0) {
                sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), request.getNotifyContent());
            } else {
                WFCMessage.MessageContent content = null;
                if (request.getType() == Modify_Group_Name) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupNameNotifyContent();
                } else if (request.getType() == Modify_Group_Portrait) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, null, "").getChangeGroupPortraitNotifyContent();
                } else if (request.getType() == Modify_Group_Mute) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupMuteNotifyContent();
                } else if (request.getType() == Modify_Group_JoinType) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupJointypeNotifyContent();
                } else if (request.getType() == Modify_Group_PrivateChat) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupPrivatechatNotifyContent();
                } else if (request.getType() == Modify_Group_Searchable) {
                    content = new GroupNotificationBinaryContent(request.getGroupId(), fromUser, request.getValue(), "").getChangeGroupSearchableNotifyContent();
                }

                if (content != null) {
                    sendGroupNotification(fromUser, request.getGroupId(), request.getToLineList(), content);
                }
            }
        }
        return errorCode;
    }
}
