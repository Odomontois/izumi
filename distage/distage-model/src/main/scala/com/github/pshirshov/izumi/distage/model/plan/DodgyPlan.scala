package com.github.pshirshov.izumi.distage.model.plan

import com.github.pshirshov.izumi.distage.model.plan.ExecutableOp.SetOp._
import com.github.pshirshov.izumi.distage.model.plan.ExecutableOp.{ImportDependency, InstantiationOp, ProxyOp}
import com.github.pshirshov.izumi.distage.model.reflection.universe.RuntimeUniverse

/**
  * ***********,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,***./((%%&&&%%##((//////((((((((((((((((((((((///(((((((((//////(((
  * **************,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,***./((%%%&%###((////////(((((((((((((((((((((//(((((##############
  * .////8**************,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,**./((%%(*,***********./////////////////////////(((#%%%%%%%%%%%%%%
  * *./////////8*********,,,,,,,,,,,,,.,,,,,,,,,,,,,,,,,**.//(/.,***************.////////8*././/////.////(((%%&&&&&&&&&&&&
  * ***.////////////////8***,,,,,,,,,,,.....,,,,,,,,,,,,**./8,.,*.//////8**,./8.//////////8********.//////((#%&&&&&&&&&&&&
  * ****./////////////////8***,,,,,,,,,,......,,,,,,,,,,****. .,*.//////8***./////((((((/////////////////(((#%&&&&&&&&&&&&
  * ./8**.////////////////////8***,,,,,,,,,......,,,,,,,,,**,...,*.//////8***,,.//((((((((///////(///////((((#%%%&&&&&&&&&&&
  * (/////////////////////////8***,,,,,,,,......,,,,,,,,,,. .,,.////////8**,,,.///((((((((/////////((///(((##%&&&&&&&&&&&&&
  * #####((((((((////////////////8**,,,,,,.......,,,,,,,. ..,*.////////8**,./8,*.////////////////////////((##%&&&&&&&&&&&&&
  * ##########(((((((////////(////8**,,,,.......,,,,,,,, ..,**.////////8***./8,**.////////////////.///////((#%&&&&&&&&&&&&&
  * ((############((((((((/(((((////8**,,.......,,,,,,, ...***.////////8**,.//,*./////((/////////////////(((#%&&&&&&&&&&&&&
  * ***.//(((#####((((((((((((((///8*,,,.....,,,,,,,.  ..****.////////8***./8,.///(((((((((((((///////(((((#%%%&&&&&&&&&&&
  * ,*****./////((((((((((////////8***,,,,,,,,,,,,,,,   .,***./////////8**,***,.///((((((((((((((((((((((((((##############
  * .////////////////////////////////8***************.  .,***.//////////8**,***.////////////////(((((((((////(((((((((((((((
  * ((((((((((((/((((((((((((((((((((((/(/////////8.  ..***.///////////8**,***.////////////////////////////////////////////
  * ############################################(*   ..,**.////////////8**,***((((((((((((((///////////////////////////////
  * %%%%%%%%%%%%%%%%%%%%%%%%&&&&&&%&&&%&&&%%%%%%/   .,,,///////////////8***./#################(((((((((((((((((((//////////
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%     ..,.//(//////////8*****(&&&&&&&&&&&&&&&%%%%%%%%%%%%###########((((((((
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%,    ..,*./////////////8****,(&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%%%%%%%%%
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%*   ..,,*.//////////////8**,,./%&&&&&&&&&&&&&&@&&@&&@@&&@@@@@@@@@@@@@@@@@@@@@
  * &&&%&&&&&&&&%&&&&&&&&&&&&&&&&&&&&&&&&&&&%/    ..,**./////////////8**,,***#&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&@&@@@@@@@@@@@@
  * &%&&&&&&&&&&&&&&&%&&&&&&&&&&&&&&&&&&&&&.   ..,**.//////////////8*****,,(&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&@@@@&&@
  * %&%&&&&&&%&&%&%%&%%%%&%&&&&&&&&&&&%%%%%%.   ..,,*.///////////////8***,**,(&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&@@@@
  * &%%&&&&%&&&&%%%%%%%%%&%%&%%%%%%%%%(,,........,,,,*******.///////8****,,,,/&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&@&&&&
  * %%%%%&%%%%%%%%%%%%%%%%%%%%%%%%%%%*      .,,,*****.//////8************,,,,/&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
  * %%&%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*     .,*.//////(((((((((/////8***,,,,,,/%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&(.   ..,.//((((((((((((((((((/////8,.  .,,*(%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%,   ..,.//(((((((######(((((((////8,,..,,./%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&. ..,,*./(((((#########((((((((///8**,,**(%&&&&&&&&&&&&&&&&&%%%%%&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&%%%%%%%%%%%%%%%%&&&&&&&&&%%/. ..,,,*((((#############(((((////8***./#%&&&&%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%&
  * &%%%%%%%%%%%#%%%#%#####%%%%%%%%%%%%%/..,*.//((((((########((((((/////8.///(#%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%###########%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%#%%%%##    .,,./((###########(((((((//////(((//8****,*#%%%%%%%%%%%%%%%%%%###############%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%###(.../8***.//((((##%%&&&&&&&%(*.,,,*.//////////8,*#%%%%%%%%%%%%%%%##################
  * %%%%%%%%%%%%%%%%%%%##############%###. *(((/8.//(((#%&@@@@@&&&%(/8,,,,**.//////////8,*(##%%%%%%%%%%%########%%%#######(
  * %%%%%%%%%%%%%%%##((///8*******.////((,  .(##/.//((%&@@&&@&((/8**,...,*./////////8*.   ../((####%%%%#%%%%%%%###((/////
  * #%%%%%%%%%%%%%%##((//8******,,,,*****,...,****./((#&%%%###(((//8**, ..,*./////////8*.     .,.///((##%%%%##%####(//8****
  * ##%%%%%%%%%%%%%%%###((///8***********, ..,,***,.//(((((((((##((/8,,...,*./////////8,.      .,,**./((####%######(/8*****
  * ##%%%%%%%%%%%%%%%%%%%%###(((((////////(#*.,,**,.////(((#%&&&&(/8,,..,*.////////8*,,.     ..,**.//(####%%######((/////
  * %#%%%%%%%%%%%%%%%%%%%%%%%%########((((/,,...,,.,.////((#%&&@@&/8,,..**./////////8,*,.   ..,,./(((#####%%##%#%%#######
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%##,    ....,*.///((#%&&@@/8**,.,*./////////8****...,,,,*(#####%%%%%%%%%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%,      ...,**.///(((##((//8**,,,,*./////////////8*****,,/%%%%%%%%%%%%%%%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%.      ..,,**.////((/////8***,,,*.///////////////////,,/#%%%%%%%%%%%%%%%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%/      .,,,**.//////(/////8***,,*.///(((/////////////8*./(%%%%%%%%%%%%%%%%%%%%%%%%
  * %%%%%%%%%%&%%%%%%%%%%%%%%%%%&%%%%%%%%#.    ..,,,***./8*.//(/////8***,*.//((((//./////((((/8****./%%%%%%%%%%%%%%%%%%%%%%
  * %%%%%%%&%%%%&&&%%&%%%%%%%%%%%%%%%%%%%%/.   .,,,**./////8./(/////.//8,,.///(((/8*.////((((/8*****./#%%%%%%%%%%%%%%%%%%%%
  * %%%%%%%%%%&&&&&&&%&&&&&%&&%%&&%%%&%%%%%*  ..,,,,,.///////(/////8.//8***.//(/8*.////(((((/8.//8***,,/%%%%%%%%%%%%%%%%%%%
  * %%&%%%%&&&&&&&&&&&&&%&%&&&%&&&&&%%%&%%(. ..,,,,,,.///(((//////8*.//8***./(/8**.////((((//.////8**,,*(#%%%&%%%%%%%%%%%%%
  * %&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%&%%,  ..,,,,,**.//////////.////8***.//(/.//////(((((////////((///8****#%&%%%%%%%%%%%
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%%%#.  .,,,,,,***./////////////8****.///////////(((((//((/(((#(///////8**(%&&&&&%%%%%
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%/..,,,...,,**.////////////8***.///////////(((((((((((((#%#(((//////8***#%&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%(.    ..,,,,.////////////8**.///(((((//((((((((((((((##%#((((//////8***#&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%*     ..,,,,***.////////8**.///((((((((((((##(((((((##%##((((((/////8*./%&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%*    .....,,,**.////////8*.//./(((((((##((#(####((####%##(((((((/////8*,*#&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&(.  ..,....,,,*.///////8*.///8./((((((################%%##(((((((((///8..,#&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&,   .,,,...,,,*.//////8******.//(((((################%%#(((((((((((//8,  ,#&%&&%%%
  * @@@@@@&@@@@@@@@@@@@@@@@@@@@@@@@@@@&&%   ..,,,,...,*.//////8*./((/.///((((((((####(#######%%%#(((((((((((//8. .*#%%%%%%%
  * @@@@@@@@@@@&&&&&&&@&&&&&&&&&&&&&&&&%*  ..,,,****,,*.///8*..*./((((((((((((((((#((((#####%%%(((((((((((((/8,..,/%%%%%%%%
  * &&&&&&&&&&&&&&&&&&%%%%%%%%%%%%%%%%%/  ..,,,,*.//(###((///8.,./((((((((((((((((#((((#####%%#(((((((((((((/8,,,*#%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(.  .,,,,*.//((#####((//8,.//(((((((((((((((#((((####%%#(((((((((((((//8***(#%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%#*  .,,,,**.//(((((###(((///////(//((((((((((#(((#####%#((((((((((((((//8*./#%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%#*  ..,,,**.///((((#%%##((((((((((((((((((((((#(((###%##/8*.///((((((((////(#%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(.  .,,****.//((//#%%%%##((((((((((//((((((((###(###%%/...,*.//((((((/(////#%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%(.  ..,,***.//////(%%%%%###(((((((((((((((((((########*....,.///(((((((((/(#%%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%#,  ..,,***.//////(#%%%%%(((#((((((((((((((#((#######(, ...,.////((/(((((//#%%%%%%%%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%*  ..,,*********./(#%%%%%(**(#((((((#(((((##########*....,,.///////((((((((%%%%%%%&%%%%%%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%%#  ..,,,,,*.///8**./////(##*,/##(((((#((############*....,*.///////((((((((%%%%&&%&&&&&&&%
  * %%%%%%%%%%%%%%%%%%%%%%%%%%%%#*  ...,.///////((//8./////((//((##(((#############/. ...,*.////////(((((//(%&&&&&&&&&&&&&&
  * %%%%%%&&&%%%&&%&%&%%%%%%%%%#,  .,./////(((////./((####/8((((((###############(.  ..,*.////////((((((/8./%&&&&&&&&&&&&&&
  * &%%%&&&%&%&&&&&%&%%&&&&&%%%* .,./////((////8**#%%%%%##(/(###################,.....,.///////////(((///8./%&&&&&&&&&&&&&&
  * %&&&&&&&&&&&&&&&&&&&&&&&&%*.../////((((((#%%#./#%%%%%%#(#%%%%%############*. ...,*.////////////((////8,*#&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&%*,.////(((((##%%%%%%(./%%%%%(%&%&&%(##########*....,**.////////////((((((/8,*#&@@@@@@@@@@@@@
  * &&&&&&&&&&&&&&&&&&&&&&%/,./////(((##%%%%%%%%%#/8(%&&&(%&&&#%%#(####/8****./////////////((((((#((/8**(@@@@@@@@@@@@@@
  * &&&&&&&&&&&&&&&&&&&&&%*,./////((((#%&&%%%%%%%%(/(%&&&#%&&&%&&%(##(////////////////////((((((###((/8,/@@@@@@@@@@@@@@
  * &&&&&&&&&&&&&&&&&&&&%/.///((((##/8./#%&%%%%&%%%//%&&&#&&&&%#&&&%#((///////((////////////(///(####((/8**&@@@@@@@@@@@@@
  * &&&&&&&&&&&&&&&&&&,,///(((###%%%(/./(%%%&%%%%(/%&&&%#%&&@%&&%#((//////((((((////(((((((//(#%###((/8**%@@&&&&&&&&&&&
  * &&&&&&&&&&&&&&@&&%*,.//(((###%%&&&&%#/8*(%&&%&(#%&&%#&@&@%%&%#(((((/((((((((/////(((((((((((######(/8**(&&&&&&&&&&&&&
  * @@@@@@@@@@@@@@@@%*,.//((###%%%&&&&&&&%#/8(%%&&(#%&%&@@&%##((((((/(((((/(((////((((((((((########((/8./%&&&&&&&&&&&&
  * @@@@@@@@@@@@@@@&*,.//((###%%%&&&&&&&&&/8((%&(#&&%%%&&%#(((((((((((((((/(((((((((((((((##########((/8./#&&&&&&&&&&&&
  * @@@@@@@@@@@@@@@,..//((###%%&&&&&&&&&%%&//(%&##&@%%%%#(((((((((((((((((((((((((((((((((######%###((/8**(&&&&&&&&&&&&
  * @@@@@@@@@@@@@&*..//(###%%%&&&&%%%%&&&&&&((%&#%@##((((((//(((((((((((((((((((((((((#%%#####%####((/8./#&&&&&&&&&&&
  * @@@@@@@@@@@&&/.*(((##%%%%&&%%%%%%&&&%#&&&(#&%%%&%###(((((((((((((((((((((((((((((((((#%%####(#%####((/8**(%&&&&&&&&&&
  * &&&&&&&&&&&&(..(((###%#((//////////(#/(%&(##%%&&%%###((((((((((((((((((((((((((((((((#%%%#####%####((//8./%&&&&&&&&&&
  * &&&&&&&&&&,,/(###%%#####%%%%%%#(///(((#%##%&&&@&%%%####((((((((((((((((((((((((((((###%%%######%####((/8./(%&&&&&&&&&
  * &&&&&&&&&&%/./((###%%%%%&&&&&&%%&&&%#((((#%%%&&@&%#########(((((((((((((((((((((((((####%%%######%####((/8.//#&&&&&&&&&
  * &&&&&&&&&&(./((##%%%%%%&&&&&&&&&&&&&&%##%#%%%%#((((((###%###(((((((((((((((((((((((#####%%%%#####%####((/8.//(#&&&&&&&&
  * &&&&&&&&&%/./((#%%%%%&&&&&&&&&%%%&&&&%%%%&%#((///8*./((##%%####(((((((((((((((((((#%%###%%%%#####%#####(/8*.//(%&&&&&&&
  * &&&&&&&&%/./((#%%%%%&&&&&&%%%%%%######%&&&%#((//8,.,.//((#%%%####(((((((/((((((((#%%%###%%%%#####%%####((/8.//((%%&&&&&
  * &&&&&&&./((##%%%%%&&&&%%#((((((((##%&&&&%%##((////////((#%%%####((((((((((((((#%%%%%##%%%%#####%%####((/8.///(#&&&&&&
  * &&&&&&&%/./(###%%%%&&&%#(///(#%%%&%%%&&&@&&%%%####((/////((%%%%####((((((((((((#%%%%%%##%%%%#####%%%###((/8.///((%&&&&&
  * &&&&&&./((##%%%%&&%#///#%%&&&&&&&%&&&@&%%%%#((((////////(#%%%####(((((((((((#%%%%%%%##%%%%%####%%#####((/8.///(#&&&&&
  * &&&&&&&/./(##%%%%%%(//#%&&&&&&&%%#%%&&@&%#(((//8...//(////(#%%%%####((((((((#%%%%%%%%%%%%%%%%####%%%####((/8*.//((%&&&&
  * &&&&&&&//((##%%%#(/(#%&&&&&&%#####%&&&&&%#((//8*.///(((((/(##%%%%####((((((#%%%%%%%%%%%%%%%%%%###%%%#####((/8.//((%&&&&
  * &&&&&&%//(##%%%#(#%&&&&&&&&&%###%&&&&&&&&%##(////(((//((((/(#%&%%%####((((#%%%%%%%%%%%%%%%%%%%###%%%#####((/.///(#%&&&&
  * &&&&&/(##%%%%%&&&&&&&&&&%###&&@&%&&@@&@&%%#(((((///(((((/(%&&&%%%###(((#%%%%%%%%%%%%%%%%%%%%###%%%%####(((////(#%&&&&
  * &&&&&/(#%%%%%&&&&&&&&%#(#%&@@&%%%&&@@@@&%%%#((//./((((//(%&&&&&%%#####%%%%%%%%&%%%%%%%%%%%%%%##%%%%%####((///((#&&&&&
  * &&&&&/##%%%%%&&&&&&((#&&@@%%%%&&&@@@@&%%#((////////(//#&&&&&&%%####%%%%%%%%%&&%%%%%%%%%%%%%##%%%%%####(((((((%&&&&&
  * &&&&&(##%%%&&&&&&&(#%&&&&%%%&@&&&&&&@@&%##((/(((//((//%%&&&&&%%###%%%%%%%%%%&&%%%%%%%%%%%%%#%%%%%%%###((((((#&&&&&&
  * &&&&&(#%%%%&&&&&&%((%&&@@&%%%&@&%&&&&&&&&&&%%####(/(((/(#(#%&%%%%##%%%%%%%%%%&&%%%%%%%%%%%%%%%%%%%%%%####(((((%&&&&&&
  * &&&&&(#%%%%&&&&((%&&&&&%#%@@&%%&@%%&@&&&@&%%###((#(//%&%#(/((####%%%%%%%%%%&&&%%%%%%%%&%%%%%%%%%%%%%###(((#%&&&&&&&
  * &&&&&(#%%%%&&&%#(#&&&&&#%&@&%%&@%&@%%%&&&&&%%%%#((#&@@@((####%%%%%%%%%&&&&&%%%%%%&&%%%%%%%&&%%%%%###%&&&&&&&&&&
  * &&&&&&%(##%%&&&%((%&&&&&#%@@&%%&@#%&#(&&&&@&&&&&&&&&@@@@@@&%%%%%%%%%&%%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&((#%%%&%(#%&&&&&%((%&&&%#%&&%##&&&%%(%&&&&&&&&&&&&&&&&&@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@&&&&&&%%%&&&&&&&
  * &&&&&&(#%%%&&%&&&&&&(%&&&#&&&%##&&&&(%&&&&&&&&&&&%%&&&&&&&&@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@&&&&&&&&%%%&&&&&&&
  * &&&&&&&%(##%%&&&&&&&&(%&&&&%(%&&&%&&&&&&((%&&&&&&&&%%%%%%%&&&&&&&&&&&@&&&&&&@@&&@@@@@@@@@@@@@&@@@&&&&&&&&&%%&&&&&&&
  * &&&&&&&(#%%%&&&&&&&%(%&&&&#&&&&&(%&&&&&&%((#&&&&&&&%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%%%&&&&&&&
  * &&&&&&&&(#%%&&&&&&#&&&&&%(%&&&&&(%&&&&&&&&%#%&&&&&&%%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%%%%%%%%%&&&&
  * &&&&&&&&&&%##%%&&&&(&&&&&&%(&&&&&#&&@@&&&&&&&&&&&&&&%%%%%%%%%%&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&%%%%%%%%%%%%%%
  * &&&&&&&&&&&%##%&&&&%#%@&&&&(&&&&&#&@@@@@&&&&&&&&&&&&&%%%%%%%%%&&&&&&&&&@@@@@@@@@@@@@&&&&&&&&&&&&&&&&&&&&&%%%&&&&&&&
  * &&&&&&&&&&&&%%#%%%%#%&@&@&&&(#&&&&&@%(%&&@@@@@@@@@@@@&&&&&&%%%%%%&&&&&&&@@@@@@@@@@@@@@@@&@@@@@@&&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&%%%%&&&&&@&&%(%@@@@@&%@@@@@@@@@@@@@@@@&&&&%%%%%%&&&&&&&&@&&@&&&@@@@@@@@&@@@@@@@&&&&&&&&&&&&&&&&&&&&&&&
  * &&&&&&&&&&&&&&&&&&&&&&&&&&&%#&&&&&&@@@@&&&&&&&&&&&&&@@@&&&&&%%%%%%&&&&&&&&&&@@@@@@@@&@@@&&@@&&&&&&&&&&&&&&&&&&&&&&&&&&&
  **/
case class DodgyPlan(
                      imports: Map[RuntimeUniverse.DIKey, ImportDependency]
                      , sets: Set[CreateSet]
                      , proxies: Seq[ProxyOp.InitProxy]
                      , steps: Seq[InstantiationOp]
                      , issues: Seq[PlanningFailure]
                    ) {
  def statements: Seq[ExecutableOp] = imports.values.toSeq ++ sets.toStream ++ steps ++ proxies

  override def toString: String = {
    val repr = issues.map(_.toString) ++ statements.map(_.format)
    repr.mkString("\n")
  }

}

object DodgyPlan {
  def empty: DodgyPlan = DodgyPlan(Map.empty, Set.empty, Seq.empty, Seq.empty, Seq.empty)
}
