package wayzer.user

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.entities.type.Player
import mindustry.game.EventType
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun getIcon(level: Int): Char {
    if(level<=0)return (63611).toChar()
    return (63663 - min(level, 12)).toChar()
    //0级为电池,1-12级为铜墙到合金墙
}

fun level(exp: Int) = floor(sqrt(max(exp,0).toDouble())/10).toInt()
fun expByLevel(level:Int) = level*level*100

registerVarForType<PlayerProfile>().apply{
    registerChild("totalExp","总经验",DynamicVar{obj,_->obj.totalExp})
    registerChild("level","当前等级",DynamicVar{obj,_->level(obj.totalExp)})
    registerChild("levelIcon","当前等级图标",DynamicVar{obj,_->getIcon(level(obj.totalExp))})
    registerChild("nextLevel","下一级的要求经验值",DynamicVar{obj,_-> expByLevel(level(obj.totalExp)+1)})
}

fun updateExp(p:Player,dot: Int): Boolean {
    val profile = transaction {
        PlayerData[p].profile?.apply {
            if (dot != 0){
                totalExp += dot
                if(level(totalExp)!=level(totalExp-dot)){
                    p.sendMessage("[gold]恭喜你成功升级到{level}级".with("level" to level(totalExp)))
                }
            }
        }
    }
    p.name = p.name.replace(Regex("<.>"), "<${getIcon(level(profile?.totalExp ?: 0))}>")
    return profile != null && dot != 0
}
export(::updateExp)

listen<EventType.PlayerJoin> {
    it.player.apply {
        name = "[white]<.>[#$color]$name"
        updateExp(this,0)
        sendMessage("[cyan][+]{player.name} [gold]加入了服务器".with("player" to it.player))
    }
    broadcast("[cyan][+]{player.name} [goldenrod]加入了服务器".with("player" to it.player))
}
