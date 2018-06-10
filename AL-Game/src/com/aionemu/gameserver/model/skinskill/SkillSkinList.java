/**
 * This file is part of Aion-Lightning <aion-lightning.org>.
 *
 *  Aion-Lightning is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Aion-Lightning is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details. *
 *  You should have received a copy of the GNU General Public License
 *  along with Aion-Lightning.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aionemu.gameserver.model.skinskill;

import java.util.Collection;

import javolution.util.FastMap;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerSkillSkinListDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.SkillSkinTemplate;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SKILL_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * @author Ghostfur (Aion-Unique)
 */
public class SkillSkinList {
	private final FastMap<Integer, SkillSkin> skillskins;
	private Player owner;

	public SkillSkinList() {
		skillskins = new FastMap<Integer, SkillSkin>();
		owner = null;
	}

	public Player getOwner() {
		return owner;
	}

	public void setOwner(Player owner) {
		this.owner = owner;
	}

	public boolean contains(int skinId) {
		return skillskins.containsKey(skinId);
	}

	public void addEntry(int skinId, int remaining, int active) {
		SkillSkinTemplate ss = DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(skinId);
		if (ss == null) {
			throw new IllegalArgumentException("Invalid skill skin id " + skinId);
		}
		skillskins.put(skinId, new SkillSkin(ss, skinId, remaining, active));
	}

	public boolean addSkillSkin(int skinId, int time) {
		SkillSkinTemplate ss = DataManager.SKILL_SKIN_DATA.getSkillSkinTemplate(skinId);
		if (ss == null) {
			throw new IllegalArgumentException("Invalid skin id " + skinId);
		} if (owner != null) {
			SkillSkin entry = new SkillSkin(ss, skinId, time, 0);
			if (!skillskins.containsKey(skinId)) {
				skillskins.put(skinId, entry);
				DAOManager.getDAO(PlayerSkillSkinListDAO.class).storeSkillSkins(owner, entry);
			} else {
				PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_MSG_COSTUME_SKILL_ALREADY_HAS_COSTUME);
				return false;
			}
			PacketSendUtility.sendPacket(owner, SM_SYSTEM_MESSAGE.STR_MSG_GET_ITEM(ss.getName()));
			PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
			return true;
		}
		return false;
	}

	public void removeSkillSkin(int skinId) {
		if (!skillskins.containsKey(skinId)) {
			return;
		}
		skillskins.remove(skinId);
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).removeSkillSkin(owner.getObjectId(), skinId);
	}

	public void setActive(int skinId) {
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).setActive(owner.getObjectId(), skinId);
		owner.setSkillSkinList(DAOManager.getDAO(PlayerSkillSkinListDAO.class).loadSkillSkinList(owner.getObjectId()));
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
	}

	public void setDeactive(int skillId) {
		int skinIdToremove = 0;
		SkillTemplate skillGroup = DataManager.SKILL_DATA.getSkillTemplate(skillId);
		if (this.owner.getSkillSkinList() != null) {
			for (SkillSkin skillSkin : owner.getSkillSkinList().getSkillSkins()) {
				if (skillSkin.getTemplate() != null) {
					if (skillSkin.getTemplate().getSkillGroup().equalsIgnoreCase(skillGroup.getSkillGroup()) && skillSkin.getIsActive() == 1) {
						skinIdToremove = skillSkin.getId();
						break;
					}
				}
			}
		}
		DAOManager.getDAO(PlayerSkillSkinListDAO.class).setDeactive(owner.getObjectId(), skinIdToremove);
		owner.setSkillSkinList(DAOManager.getDAO(PlayerSkillSkinListDAO.class).loadSkillSkinList(owner.getObjectId()));
		PacketSendUtility.sendPacket(owner, new SM_SKILL_ANIMATION(owner));
	}

	public int getSkinId(int SkillId) {
		int skinid = 0;
		if (SkillId == 0 || getOwner().getSkillSkinList() == null || getOwner() == null) {
			return 0;
		}
		for (SkillSkin skillSkin : getOwner().getSkillSkinList().getSkillSkins()) {
			if (DataManager.SKILL_DATA.getSkillTemplate(SkillId).getSkillGroup() != null) {
				if (skillSkin.getTemplate().getSkillGroup().equalsIgnoreCase(DataManager.SKILL_DATA.getSkillTemplate(SkillId).getSkillGroup()) && skillSkin.getIsActive() == 1) {
					skinid = skillSkin.getId();
				}
			}
		}
		return skinid;
	}

	public int size() {
		return skillskins.size();
	}

	public Collection<SkillSkin> getSkillSkins() {
		return skillskins.values();
	}
}
