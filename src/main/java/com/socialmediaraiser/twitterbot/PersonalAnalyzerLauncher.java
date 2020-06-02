package com.socialmediaraiser.twitterbot;

import com.socialmediaraiser.twitterbot.impl.personalAnalyzer.DataArchiveHelper;
import com.socialmediaraiser.twitterbot.impl.personalAnalyzer.PersonalAnalyzerBot;
import java.io.IOException;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersonalAnalyzerLauncher {

    private static final Logger LOGGER = Logger.getLogger(PersonalAnalyzerLauncher.class.getName());

    public static void main(String[] args) throws IOException, InterruptedException {
        if(args.length<2){
            LOGGER.severe(()->"missing arguments, expecting 2 : ownerName[String], unfollowMode[boolean]");
        } else{
            String userName = args[0];
            boolean unfollowMode = Boolean.parseBoolean(args[1]);
            PersonalAnalyzerBot bot = new PersonalAnalyzerBot(userName, userName.toLowerCase()+"-tweet-history.json");
            if(!unfollowMode){
                boolean includeFollowers = true;
                boolean includeFollowings = true;
                boolean onlyFollowBackFollowers = true;
                String tweetArchivePath = userName.toLowerCase()+"-tweet-history.json";
                if(args.length>5) {
                    includeFollowers = Boolean.parseBoolean(args[2]);
                    includeFollowings = Boolean.parseBoolean(args[3]);
                    onlyFollowBackFollowers = Boolean.parseBoolean(args[4]);
                    tweetArchivePath = args[5];
                }
                bot.launch(includeFollowers, includeFollowings, onlyFollowBackFollowers, tweetArchivePath);
            } else{
                String[] toUnfollow = {
"sxrnek","nnes75","TarekGstvo","Bsalva57","FatiimK","Anissa_BBoomBox","lkosovard","qpaef","hakumei__","Ryku_78","k_douniaa","__Yousraaa","___Assia",
                        "_Maravish07","Rayan_air95","linco8_","speranceee","d_aleeeeex","liyendrias","YvelinoiseFr","Astiyb","mrgxcrt","Zifo_11","PatrickMany",
                        "Enzzz94","mavou02","mohaamed212","Ange_MKM","Nac_amr","Naass___","Metizia7","Poupits_","Butwhy88","AlizoouBondy","loicll","iamthaissou",
                        "fayelams","hmlnsn","G_RY_","Lolo_lblc","Debo_sy","ibtissemTNS","seygalare","assicmoi","Ms_Enk","PerfeectHeaven","alaprod_","yohann330",
                        "mllekind","dslbvby","dydyi92","traffy_c","IsabelleTouihri","maximeee45","manelQo","mimiipou","_hvsna","kkfeer","NacimaMgh","z_oldyck",
                        "nessssi95","JAVADDOUT","emmatouu","mabiche_11","oumaimadu84","la7serra","rrhardaway","Fadxabk","_CeliaNB","blackchamyyraa","njwel",
                        "OceaneTassart","weishenniste","Zentene93","6__Mel","ninomatador91","njmamn","PasComprise","MehdiMoiStp","pl_lallement","nour__75",
                        "naiyhana","lea_pierre_","mrskaizen","fhrmi","mathildedsa","JDCFernandes","basamSysy","Le_Cooorse","Mallorie_Alex","AliciaParsy",
                        "bestellec","Katell_brk","dymzi_","YacShelby","aigledCarthage","sosomaness06","Janine_Srs","DiallaD","Mangasmaniac","Sana_linaa",
                        "CehimP","sabcbienaussi","DaoFav1te","helofrc","__Bouchraa","elena_guerraf","_notofuckingkay","Zaranoun","Simisco_","laetifr14",
                        "nassb0","pommegranata","juls_eve","OtmanBsh","rifia_tounsia","PockyHv","Yaass_Hrk","vivafranca","phoenix_iia","Nassim_dz213",
                        "Dr_Chaay","MohamedLagab","AmineMB7","Boz7rt","samantha_bvx","melkk__","ThomasJota","ftguuuul","AnassAlthani","PG103","RebeuMane",
                        "EveAngela1","_Manzola","Azanian_sunbae","carolynnrdgt","tchichamermez","Trtflladr","hicrandl","ScherAmandine","rhaisbkl","Ti_Jay_","YSerena_","Hugobldl","bychvrn","yahyou_","valoche2392","bosnianjiraya","asyaelm1","m_hwj","Staifi93","fidunyaa","KevinDYass","SuperChleuhMan","hamza59160","K1NGZEYKO","_krrsln_","itwasalladrm","DamienCdr","FrenchieAC","islemhxh","lauraleesan2","chainxze","masillia13","cyrinenb","OphelieEdb","Mehdietdemi2","Celia_Tvbti","nightlxver","Mouradson","suedois_","MO_me_Z","shaybk_","cherifa_ma","bblckandj","SaraLeenHayder","DaChelha","wenillicite","ala_noorii","sof1aninho","GLeYeti77","Irfane_Clement","GiuseppeParavi1","NaelABC","Vfrican_psycho","RyadBenaidji","ayumeiro","vivi83032363","Sarah_Bcf","Dolceeevitax","ferme_ta__g","ayss4pf666","AdjafatouK","SoPrz","cyclozu","sxnshemmings","Whatsupcesca","El_Svhraoui","Naouffel","julien_Gres"
                };
                String[] whiteList = {"InesInesz","caroletonneau","KayiTurk_fr","Asiorak","Sarah_Bcf","vivi83032363"
                        ,"Gabriel_Bleron","NaelABC","Ciudadana93","Irfane_Clement","OneblueTeam","Luffy_Affranchi","silversilvery",
                        "abdelhvmid","souf_belkadi","GiuseppeParavi1","Riad7ben","YSerena_","Celia_Tvbti","sombrenuance",
                        "salmaechouay","VictorLefranc","julien_Gres","rk_Ngadi","FrenchieAC","OtmanBsh","sof1aninho",
                        "SaraLeenHayder","Mouradson","OphelieEdb","Naouffel"};
                bot.unfollow(toUnfollow, whiteList);
            }
        }
    }


}
