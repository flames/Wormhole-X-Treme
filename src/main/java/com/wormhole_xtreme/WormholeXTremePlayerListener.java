/*
 *   Wormhole X-Treme Plugin for Bukkit
 *   Copyright (C) 2011  Ben Echols
 *                       Dean Bailey
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.wormhole_xtreme; 

import java.util.logging.Level;


import org.bukkit.Location; 
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener; 
import org.bukkit.event.player.PlayerMoveEvent; 

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;
import com.wormhole_xtreme.config.ConfigManager;
import com.wormhole_xtreme.config.ConfigManager.StringTypes;
import com.wormhole_xtreme.model.Stargate;
import com.wormhole_xtreme.model.StargateManager;


// TODO: Auto-generated Javadoc
/**
 * WormholeXtreme Player Listener.
 *
 * @author Ben Echols (Lologarithm)
 * @author Dean Bailey (alron)
 */ 
public class WormholeXTremePlayerListener extends PlayerListener 
{ 
	
	/** The wxt. */
	private WormholeXTreme wxt = null;
	//private ConcurrentHashMap<String, Integer> PlayerCompassOn = new ConcurrentHashMap<String, Integer>(); 
	//private final WormholeXTreme plugin;
	/**
	 * Instantiates a new wormhole x treme player listener.
	 *
	 * @param instance the instance
	 */
	public WormholeXTremePlayerListener(WormholeXTreme instance) 
	{ 
		//plugin = instance; 
		wxt = instance;
	}
 

//	private void PrintHelpFile(Player player) 
//	{
//		player.sendMessage("Commands are: help, remove <name>, material <material>, irismaterial <material>, perms/perm, active_timeout <time>, shutdown_timeout <time>, owner <gate_name> <optional_set_owner>");
//	}

	/* (non-Javadoc)
	 * @see org.bukkit.event.player.PlayerListener#onPlayerMove(org.bukkit.event.player.PlayerMoveEvent)
     */
	@Override
    public void onPlayerMove(PlayerMoveEvent event)
	{
		Player p = event.getPlayer();
		Location l = event.getTo();
		Block ch = l.getWorld().getBlockAt( l.getBlockX(), l.getBlockY(), l.getBlockZ());
		Stargate st = StargateManager.getGateFromBlock( ch );
		 
		

		if ( st != null && st.Active && st.Target != null )
		{
		    String gatenetwork;
		    if (st.Network != null )
		    {
		        gatenetwork = st.Network.netName;
		    }
		    else
		    {
		        gatenetwork = "Public";
		    }
			wxt.prettyLog(Level.FINE, false, "Player in gate:" + st.Name + " gate Active: " + st.Active + " Target Gate: " + st.Target.Name + " Network: " + gatenetwork );
			
			if ( WormholeXTreme.Permissions != null)
			{
			    // If use permission is also teleport permission we should check here:
				if (ConfigManager.getWormholeUseIsTeleport() && !ConfigManager.getSimplePermissions() && 
					((st.IsSignPowered && !WormholeXTreme.Permissions.permission(p, "wormhole.use.sign")) || 
					(!st.IsSignPowered && !WormholeXTreme.Permissions.permission(p, "wormhole.use.dialer")) || 
					(!gatenetwork.equals("Public") && !WormholeXTreme.Permissions.has(p, "wormhole.network.use." + gatenetwork))))
				{
					// This means that the user doesn't have permission to use.
					p.sendMessage(ConfigManager.output_strings.get(StringTypes.PERMISSION_NO));
					return;
				}
				else if (ConfigManager.getWormholeUseIsTeleport() && ConfigManager.getSimplePermissions() &&
				    ((st.IsSignPowered && !WormholeXTreme.Permissions.has(p, "wormhole.simple.use"))))
				{
				    p.sendMessage(ConfigManager.output_strings.get(StringTypes.PERMISSION_NO));
				    return;
				}
			}
			
			if ( st.Target.IrisActive )
			{
				p.sendMessage("\u00A73:: \u00A75error \u00A73:: \u00A77Remote Iris is locked!");
				//p.sendMessage("Remote Iris is active - unable to teleport!");
				event.setFrom(st.TeleportLocation);
				event.setTo(st.TeleportLocation);
				p.teleportTo(st.TeleportLocation);
				if (p.getFireTicks() > 0 )
				{
				    p.setFireTicks(0);
				}
				return;
			}
		
			Location target = st.Target.TeleportLocation;
			if ( WormholeXTreme.Iconomy != null )
			{
				boolean exempt = ConfigManager.getIconomyOpsExcempt();
				if ( !exempt || !p.isOp() )
				{
					double cost = ConfigManager.getIconomyWormholeUseCost();
					if (cost != 0.0) 
					{
						Account player_account = iConomy.getBank().getAccount(p.getName());
						double balance = player_account.getBalance();
						String currency = iConomy.getBank().getCurrency();
					    if ( balance >= cost )
					    {
						    player_account.subtract(cost);
						    // player_account.save();
						    p.sendMessage("\u00A73:: \u00A77Wormhole Use \u00A7F- \u00A72" + cost + " \u00A77" + currency );
						    //p.sendMessage("You were charged " + cost + " " + iConomy.getBank().getCurrency() + " to use wormhole." );
						    double owner_percent = ConfigManager.getIconomyWormholeOwnerPercent();
						
						    if ( owner_percent != 0.0 && st.Owner != null )
						    {
							    if ( st.Owner != null && iConomy.getBank().hasAccount(st.Owner))
							    {
								    Account own_acc = iConomy.getBank().getAccount(st.Owner);
								    own_acc.add(cost * owner_percent);
								    // own_acc.save();
							    }
						    }
					    }
					    else
					    {
						    p.sendMessage("\u00A73:: \u00A77Not enough " + currency  + "! - Requires: \u00A72" + cost + " \u00A77- Available: \u00A74" + player_account.getBalance() + " \u00A77" + currency);
						    //p.sendMessage("Not enough " + iConomy.getBank().getCurrency() + " to use - requires: " + cost);
						    target = st.TeleportLocation;
					    }
					}
				}
			}
			
			Block target_block = target.getWorld().getBlockAt(target.getBlockX(), target.getBlockY(), target.getBlockZ());
			while ( target_block.getType() != Material.AIR && target_block.getType() != Material.WATER && target_block.getType() != Material.LAVA )
			{
				target_block = target_block.getFace(BlockFace.UP);
				target.setY(target.getY() + 1.0);
			}
			
			event.setFrom(target);
			event.setTo(target);
			p.teleportTo(target);
			if (p.getFireTicks() > 0) 
			{
			    p.setFireTicks(0);
			}
			event.setCancelled(true);
			if ( target == st.Target.TeleportLocation )
				wxt.prettyLog(Level.INFO,false, p.getDisplayName() + " used wormhole: " + st.Name + " to go to: " + st.Target.Name);
			
			if ( ConfigManager.getTimeoutShutdown() == 0 )
			{
				st.ShutdownStargate();
			}
		}
		else if ( st != null )
		{
		    if (p.getFireTicks() > 0)
		    {
		        p.setFireTicks(0);
		    }
			wxt.prettyLog(Level.FINE, false, "Player entered gate but wasn't active or didn't have a target.");
		}
	}
} 
 
