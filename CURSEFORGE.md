# ENGRAM
##### (Previously My World.jar)
## A Vanilla-Style Slow-Burn Psychological Horror Mod
#### *Version 0.7.0-beta*

Horror lives in the familiar, and there is no game more familiar than Minecraft. Playing off your own paranoia, ENGRAM is designed to make you question even the most innocuous aspects of your world. 

This mod takes its time to play with you, letting you stew in your own fears all while something hides just out of sight, just as afraid as you are. There is a reason to all events, all building with one cohesive background narrative.

With multiplayer support, this mod will leave you unable to tell where it ends, and your game begins. There are no flashy effects, no overt modded textures. Just subtle, escalating disturbances that make you question whether you did that, or someone else did.

Join the [Discord](https://discord.gg/hgCNtXbqkC) to report bugs, give feedback, or learn more about the mod's development.

---

## Mod Content (spoilers):

<div class="spoiler">

<p>- <strong>Pillars</strong> — Something marked this place, it might come back.</p>
<p>- <strong>Explored caves</strong> — Already lit, with ores already mined.</p>
<p>- <strong>Chopped-down trees</strong> — You aren't the only one who needs wood.</p>
<p>- <strong>Punched off ledges</strong> — Knocked from cliffs by unseen hands.</p>
<p>- <strong>Footsteps behind you</strong> — You're being followed.</p>
<p>- <strong>A Not So New World</strong> — It must just be a glitch.</p>
<p>- <strong>Screenshots</strong> — You're being watched and recorded.</p>
<p>- <strong>Stolen items</strong> — Missing tools, rearranged chests, doors left ajar.</p>
<p>- <strong>Escalating aggression</strong> — The longer you survive, the bolder it becomes.</p>
<p>- <strong>Forgotten deaths</strong> — You don't remember dying here… so who did?</p>
<p>- <strong>Another player?</strong> — It feels like someone else has been building, mining, surviving…</p>
<p>- <strong>Is it your world?</strong> — What if you're the one who doesn't belong here?</p>
<p>- <strong>Settings Changed</strong> — Are you sure you turned that off? It's back on now.</p>
<p>- <strong>Multiplayer support</strong> — Experience the horror with friends!</p>
<p>- <strong>*Strip mines</strong> — Large, deep, unnatural tunnels dug beneath your feet.</p>
<p>- <strong>*Being watched</strong> — A presence staring down at you when you least expect it.</p>

<p><em>* indicates features planned for future updates</em></p>

</div>

---

## Configuration
Mod features can be configured in-game using Mod Menu, or by editing the `config/ENGRAM.json` file located in your Minecraft directory.

## Development Status

This mod is in active development. As this is my first mod, expect bugs and incomplete features. Please report any issues on the **[GitHub Issues page](https://github.com/Ice129/ENGRAM-Horror-Mod/issues)**. 

Feel free to contribute to the project! All my code is publicly available on **[GitHub](https://github.com/Ice129/ENGRAM-Horror-Mod)**.

**No Forge port is planned** for this mod, however if anyone is determined enough to do so themselves, open a PR on GitHub and I will gladly support it.

Mod Icon made by **[potatomaninyourtrashcan on Discord](https://discord.com/users/927976014972346368)**

---

*"Is it finally my time to host?"*

CISCO PACKET TRACER EXAM NOTES
email = uni email
password =

general commands:
? - display available commands or options
en / enable - enter privileged EXEC mode, used for most configuration tasks
conf t / configure terminal - enter global configuration mode, used to configure the device
hostname [name] - set the device's hostname, which is used to identify the device on the network
ip address(add) [ip address] [subnet mask] - assign an IP address and subnet mask to an interface
no shutdown(shut) - enable an interface (interfaces are disabled by default)


adding passwords:
conf t
line console 0 (configure console line, basically access controlls for the console port of the device)
password [password] 
login (enable password checking for console access)
exit 


cables:
lightning symbol - auto picks the correct cable type for the connection
straight-through cable - used to connect different types of devices (e.g., switch to router, computer to switch)
crossover cable - used to connect similar types of devices (e.g., switch to switch, computer to computer)

pc config:
go to desktop tab
ip configuration
set the IP address, subnet mask will automatically fill
usually 192.168.xx.x

router config:
en
conf t
interface [interface name, e.g., fa0/0]
ip address [ip address] [subnet mask]
no shutdown (enable the interface)
exit
REMEMBER TO SET THE DEFAULT GATEWAY ON THE PC TO THE ROUTER'S IP ADDRESS TO ALLOW COMMUNICATION OUTSIDE THE LOCAL NETWORK (/different subnet)

DHCP ON ROUTER:
en
conf t
ip dhcp pool [pool name] (create a DHCP pool with a name of your choice)
network [network address (e.g., 192.168.1.0)] [subnet mask (e.g., 255.255.255.0)] (define the network and subnet mask for the DHCP pool)
default-router [default gateway IP address] (set the default gateway for the DHCP clients)
*dns-server [dns server ip address] (optional, set the DNS server for the DHCP clients)
*lease [lease time] (optional, 1 0 0 (1 day, 0 hours, 0 minutes))
*ip dhcp excluded-address [start ip address] [end ip address] (optional, exclude a range of IP addresses from being assigned by DHCP, e.g., for static IPs)
exit
IF DHCP SERVER IS SEPERATE SUBNET: GO TO INTERFACE THEN:
int [interface name, e.g., fa0/0] 
ip helper-address [ip address] (optional, used when the DHCP server is on a different subnet.)

TO SHOW DHCP INFORMATION:
show ip dhcp pool [pool name] (to show the details of a specific DHCP pool, including assigned IP addresses)
show ip dhcp binding (to show all currently assigned IP addresses and their corresponding MAC addresses)

DHCP ON SERVER: (server-pt)
go to desktop tab
ip configuration
set static ip address for the server
set the default gateway to the router's IP address
go to services tab
enable DHCP
configure the DHCP settings (pool name, network address, subnet mask, default gateway, dns server, lease time, excluded addresses)

ENABLE DHCP ON ALL CLIENTS



devices:
1841 - default router
1950-24 - default switch

VLANS:
ON SWITCH (all of this can be done fast via gui. doing so will also show the cli commands the gui is doing in the background):
MAKING A VLAN:
en
conf t
(no [to delete vlan]) vlan [vlan number] (create a vlan)
name [vlan name] (optional, name the vlan)
exit

ASSIGNING PORTS TO A VLAN:
interface [interface name, e.g., fa0/1]
switchport mode access (set the port to access mode, which means it will only belong to one vlan)
(no [to remove the port from its current vlan]) switchport access vlan [vlan number] (assign the port to the vlan)
exit

MAKING A TRUNK PORT:
interface [interface name, e.g., fa0/1]
switchport mode trunk (set the port to trunk mode, which allows it to carry traffic for multiple vlans)
exit
do wr (to save the configuration, short for do write)

INTER VLAN ROUTING:
interface [interface name, e.g., fa0/0.10] (pysical interface aka fa0/0, but then add a .[subinterface number] for each vlan, e.g., fa0/0.10 for vlan 10)
encapsulation dot1q [vlan number] 
ip address [ip address] [subnet mask]
no shutdown (enable the subinterface)
exit

inter vlan routing:
on the router, you need to create subinterfaces for each vlan to allow communication between them.

static routing:
done only on the router, when there is more than one router in the network

- ip route [destination network] [subnet mask] [next hop ip address]

destination network: the network you want to reach, like i want to reach 192.168.3.0
subnet mask: the subnet mask for the destination network, like 255.255.255.0 - this means any ip address in the range of 192.168.3.0 to 192.168.3.255
next hop ip address: the ip address of the next router that can help you reach the destination network, basically, what router should i send the packet to in order to get it to the destination network

ACL:
Router(config)# access-list access-list-number {deny | permit | remark text} source [source-wildcard] [log]
access-list-number: a number between 1 and 99 for standard ACLs
source: the source IP address or network that you want to filter
source-wildcard: a wildcard mask that specifies which bits of the source IP address to ignore when filtering (e.g., 0.0.0.255)

ADD ACL TO INTERFACE:
interface [interface name, e.g., fa0/0]
ip access-group [access-list-number] {in | out}
in: apply the ACL to incoming traffic on the interface
out: apply the ACL to outgoing traffic on the interface

SHOW ACLS:
show access-lists (to show all ACLs configured on the router)
show access-lists [access-list-number] (to show a specific ACL)
show ip interface [interface name] (to show the ACLs applied to a specific interface)

DIAGNOSING ISSUES:
ip's with different subnets (x.x.2.x cant with x.x.1.x) cannot comunicate with each other without a router (aka not just a switch)


en
conf t
ip add [ip] [mask] (router)
no shut (enable int)
ip dhcp pool [pool name] (router DHCP pool make)
network [network address] [subnet mask] (router DHCP pool network)
default-router [default gateway] (sets gateway for DHCP clients)
dns-server
lease [1 0 0]
ip dhcp excluded-address [start ip] [end ip]
ip helper-address [ip address] (router DHCP relay for different subnet)
vlan [vlan number]
name [vlan name]
vlans = gui
interface [interface name.subinterface fa0/0.10]
encapsulation dot1q [vlan number]
ip address [ip address] [subnet mask]
no shut
ip route [destination network] [subnet mask] [next hop ip address] (static routing)
access-list [access list number] [permit/deny] [source] [wildcard mask] (wildcard is like inverse subnet mask)
int [fa0/0]
ip access-group [access list number] [in/out] (add to int)
show access-lists 
