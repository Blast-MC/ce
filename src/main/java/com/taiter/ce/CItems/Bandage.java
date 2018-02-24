package com.taiter.ce.CItems;

/*
* This file is part of Custom Enchantments
* Copyright (C) Taiterio 2015
*
* This program is free software: you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as published by the
* Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
* FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/


import com.taiter.ce.CBasic;
import com.taiter.ce.Tools;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;


public class Bandage extends CItem {
	
	int TotalHealAmount;
	int TotalHealTime;
	boolean StopAtFullHealth;
	int healBursts;


	public Bandage(String originalName, ChatColor color, String lDescription, long lCooldown, Material mat) {
		super(originalName, color, lDescription, lCooldown, mat);
		this.configEntries.put("TotalHealAmount", 10);
		this.configEntries.put("TotalHealTime", 200);
		this.configEntries.put("StopAtFullHealth", true);
		triggers.add(Trigger.INTERACT_RIGHT);
		triggers.add(Trigger.INTERACT_ENTITY);
		triggers.add(Trigger.DAMAGE_GIVEN);
	}

	@Override
	public boolean effect(Event event, Player player) {
		if(event instanceof PlayerInteractEvent) {
			AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
				if(player.getHealth() != attribute.getValue()) {
					heal(player);
					return true;
				} else
					player.sendMessage(ChatColor.RED + "You do not have any wounds to apply the bandage to!");
		} else if(event instanceof PlayerInteractEntityEvent || event instanceof EntityDamageByEntityEvent) {
			Player toHeal = null;
			
			if(event instanceof PlayerInteractEntityEvent) {
				toHeal = ((PlayerInteractEntityEvent) event).getPlayer();
			} else if(event instanceof EntityDamageByEntityEvent) {
				EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
				if(e.getDamager() == player && e.getEntity() instanceof Player) {
					e.setDamage(0);
					toHeal = (Player) e.getEntity();
				}
			}

            if (toHeal != null && !player.isDead()) {
                double maxHealth = toHeal.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                if (!toHeal.hasMetadata("ce." + getOriginalName())) {
                    if (toHeal.getHealth() < maxHealth) {
                        heal(toHeal);
                        player.sendMessage(ChatColor.GREEN + "You have applied a bandage on " + toHeal.getName() + ".");
                        toHeal.sendMessage(ChatColor.GREEN + player.getName() + " has applied a bandage on you.");
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "Your target does not have any wounds to apply the bandage to!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Your target is already using a bandage!");
                }
            }
        }
		return false;
	}

	@Override
	public void initConfigEntries() {
		TotalHealAmount = Integer.parseInt(getConfig().getString("Items." + getOriginalName() + ".TotalHealAmount"));
		TotalHealTime = Integer.parseInt(getConfig().getString("Items." + getOriginalName() + ".TotalHealTime"));
		StopAtFullHealth = Boolean.parseBoolean(getConfig().getString("Items." + getOriginalName() + ".StopAtFullHealth"));
		healBursts = TotalHealAmount / TotalHealTime;
	}
	
	private void heal(final Player p) {
		p.sendMessage(ChatColor.GREEN + "The bandage covers your wounds.");
		p.setMetadata("ce." + getOriginalName(), new FixedMetadataValue(main, ChatColor.RED + "You are already using a bandage!"));
		if(CBasic.hasCooldown(p, Tools.BLEED_META_KEY)) 
			CBasic.generateCooldown(p,Tools.BLEED_META_KEY,0);
		new BukkitRunnable() {
			int localCounter = TotalHealTime;
			@Override
            public void run() {
                if (p.isOnline() && !p.isDead() && localCounter >= 0) {
                    double maxHealth = p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
                    if (p.getHealth() == maxHealth && StopAtFullHealth) {
                        p.sendMessage(ChatColor.GREEN + "Your wounds have fully recovered.");
                        this.cancel();
                        return;
                    }
                    if (p.getHealth() + healBursts <= maxHealth) {
                        p.setHealth(p.getHealth() + healBursts);
                    } else {
                        p.setHealth(maxHealth);
                    }
				localCounter--;
				} else {
					p.sendMessage(ChatColor.GREEN + "The bandage has recovered some of your wounds.");
					this.cancel();
				}
			}
		}.runTaskTimer(main, 0l, 20l);
	}

}
