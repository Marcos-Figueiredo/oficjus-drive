"""Comparar as 298 comarcas oficiais do TJMG com nossa base de 295."""
import psycopg2, json

# 298 comarcas oficiais (extraídas da página do TJMG)
oficiais = {
    "0002":"Abaeté","0003":"Abre-Campo","0005":"Açucena","0009":"Águas Formosas",
    "0011":"Aimorés","0012":"Aiuruoca","0015":"Além Paraíba","0016":"Alfenas",
    "0017":"Almenara","0019":"Alpinópolis","0021":"Alto Rio Doce","0023":"Alvinópolis",
    "0024":"Belo Horizonte","0026":"Andradas","0027":"Betim","0028":"Andrelândia",
    "0034":"Araçuaí","0035":"Araguari","0040":"Araxá","0042":"Arcos","0043":"Areado",
    "0045":"Caeté","0049":"Baependi","0051":"Bambuí","0054":"Barão de Cocais",
    "0056":"Barbacena","0058":"Três Marias","0059":"Barroso","0064":"Belo Vale",
    "0069":"Bicas","0071":"Boa Esperança","0073":"Bocaiúva","0074":"Bom Despacho",
    "0079":"Contagem","0080":"Bom Sucesso","0081":"Bonfim","0082":"Bonfinópolis de Minas",
    "0083":"Borda da Mata","0084":"Botelhos","0086":"Brasília de Minas","0089":"Brazópolis",
    "0090":"Brumadinho","0091":"Bueno Brandão","0092":"Buenópolis","0093":"Buritis",
    "0095":"Cabo Verde","0097":"Cachoeira de Minas","0103":"Caldas",
    "0105":"Governador Valadares","0106":"Cambuí","0107":"Cambuquira","0109":"Campanha",
    "0110":"Campestre","0111":"Campina Verde","0112":"Campo Belo","0114":"Ibirité",
    "0115":"Campos Altos","0116":"Campos Gerais","0118":"Canápolis","0120":"Candeias",
    "0123":"Capelinha","0126":"Capinópolis","0132":"Carandaí","0133":"Carangola",
    "0134":"Caratinga","0137":"Carlos Chagas","0140":"Carmo da Mata","0141":"Carmo de Minas",
    "0142":"Carmo do Cajuru","0143":"Carmo do Paranaíba","0144":"Carmo do Rio Claro",
    "0145":"Juiz de Fora","0148":"Lagoa Santa","0151":"Cássia","0153":"Cataguases",
    "0155":"Caxambu","0166":"Cláudio","0172":"Conceição das Alagoas",
    "0175":"Conceição do Mato Dentro","0177":"Conceição do Rio Verde","0180":"Congonhas",
    "0182":"Conquista","0183":"Conselheiro Lafaiete","0184":"Conselheiro Pena",
    "0188":"Nova Lima","0191":"Corinto","0193":"Coromandel","0194":"Coronel Fabriciano",
    "0205":"Cristina","0208":"Cruzília","0209":"Curvelo","0210":"Pedro Leopoldo",
    "0216":"Diamantina","0220":"Divino","0223":"Divinópolis","0231":"Ribeirão das Neves",
    "0232":"Dores do Indaiá","0236":"Elói Mendes","0239":"Entre-Rios de Minas",
    "0240":"Ervália","0241":"Esmeraldas","0242":"Espera Feliz","0243":"Espinosa",
    "0245":"Santa Luzia","0248":"Estrela do Sul","0249":"Eugenópolis","0251":"Extrema",
    "0252":"Januária","0259":"Ferros","0261":"Formiga","0267":"Francisco Sá",
    "0271":"Frutal","0273":"Galiléia","0278":"Grão-Mogol","0280":"Guanhães",
    "0281":"Guapé","0283":"Guaranésia","0284":"Guarani","0287":"Guaxupé",
    "0290":"Vespasiano","0295":"Ibiá","0297":"Ibiraci","0301":"Igarapé",
    "0303":"Iguatama","0309":"Inhapim","0312":"Ipanema","0313":"Ipatinga",
    "0317":"Itabira","0319":"Itabirito","0322":"Itaguara","0324":"Itajubá",
    "0325":"Itamarandiba","0327":"Itambacuri","0329":"Itamoji","0330":"Itamonte",
    "0331":"Itanhandu","0332":"Itanhomi","0334":"Itapajipe","0335":"Itapecerica",
    "0338":"Itaúna","0342":"Ituiutaba","0343":"Itumirim","0344":"Iturama",
    "0346":"Jaboticatubas","0347":"Jacinto","0348":"Jacuí","0349":"Jacutinga",
    "0351":"Janaúba","0355":"Jequeri","0358":"Jequitinhonha","0362":"João Monlevade",
    "0363":"João Pinheiro","0372":"Lagoa da Prata","0377":"Lajinha","0378":"Lambari",
    "0382":"Lavras","0384":"Leopoldina","0386":"Lima Duarte","0388":"Luz",
    "0390":"Machado","0392":"Malacacheta","0393":"Manga","0394":"Manhuaçu",
    "0395":"Manhumirim","0396":"Mantena","0398":"Mar de Espanha","0400":"Mariana",
    "0405":"Martinho Campos","0407":"Mateus Leme","0408":"Matias Barbosa",
    "0411":"Matozinhos","0414":"Medina","0416":"Mercês","0417":"Mesquita",
    "0418":"Minas Novas","0421":"Miradouro","0422":"Miraí","0427":"Montalvânia",
    "0428":"Monte Alegre de Minas","0429":"Monte Azul","0430":"Monte Belo",
    "0431":"Monte Carmelo","0432":"Monte Santo de Minas","0433":"Montes Claros",
    "0434":"Monte Sião","0435":"Morada Nova de Minas","0439":"Muriaé","0440":"Mutum",
    "0441":"Muzambinho","0443":"Nanuque","0444":"Natércia","0446":"Nepomuceno",
    "0447":"Nova Era","0450":"Nova Ponte","0451":"Nova Resende","0452":"Nova Serrana",
    "0453":"Novo Cruzeiro","0456":"Oliveira","0459":"Ouro Branco","0460":"Ouro Fino",
    "0461":"Ouro Preto","0467":"Palma","0470":"Paracatu","0471":"Pará de Minas",
    "0472":"Paraguaçu","0473":"Paraisópolis","0474":"Paraopeba","0476":"Passa-Quatro",
    "0477":"Passa-Tempo","0479":"Passos","0480":"Patos de Minas","0481":"Patrocínio",
    "0486":"Peçanha","0487":"Pedra Azul","0491":"Pedralva","0498":"Perdizes",
    "0499":"Perdões","0508":"Piranga","0511":"Pirapetinga","0512":"Pirapora",
    "0514":"Pitangui","0515":"Piumhi","0517":"Poço Fundo","0518":"Poços de Caldas",
    "0520":"Pompéu","0521":"Ponte Nova","0522":"Porteirinha","0525":"Pouso Alegre",
    "0527":"Prados","0528":"Prata","0529":"Pratápolis","0534":"Presidente Olegário",
    "0540":"Raul Soares","0542":"Resende Costa","0543":"Resplendor","0549":"Rio Casca",
    "0554":"Rio Novo","0555":"Rio Paranaíba","0556":"Rio Pardo de Minas",
    "0557":"Rio Piracicaba","0558":"Rio Pomba","0559":"Rio Preto","0560":"Rio Vermelho",
    "0567":"Sabará","0568":"Sabinópolis","0569":"Sacramento","0570":"Salinas",
    "0572":"Santa Bárbara","0582":"Santa Maria do Suaçuí","0592":"Santa Rita de Caldas",
    "0596":"Santa Rita do Sapucaí","0598":"Santa Vitória","0604":"Santo Antônio do Monte",
    "0607":"Santos Dumont","0610":"São Domingos do Prata","0611":"São Francisco",
    "0620":"São Gonçalo do Sapucaí","0621":"São Gotardo","0624":"São João da Ponte",
    "0625":"São João Del-Rei","0627":"São João do Paraíso","0628":"São João Evangelista",
    "0629":"São João Nepomuceno","0637":"São Lourenço","0642":"São Romão",
    "0643":"São Roque de Minas","0647":"São Sebastião do Paraíso","0657":"Senador Firmino",
    "0671":"Serro","0672":"Sete Lagoas","0674":"Silvianópolis","0680":"Taiobeiras",
    "0684":"Tarumirim","0685":"Teixeiras","0686":"Teófilo Otôni","0687":"Timóteo",
    "0689":"Tiros","0692":"Tombos","0693":"Três Corações","0694":"Três Pontas",
    "0696":"Tupaciguara","0697":"Turmalina","0699":"Ubá","0701":"Uberaba",
    "0702":"Uberlândia","0704":"Unaí","0707":"Varginha","0708":"Várzea da Palma",
    "0710":"Vazante","0713":"Viçosa","0718":"Virginópolis","0720":"Visconde do Rio Branco",
    "0738":"Jaíba","0740":"Juatuba","0775":"Coração de Jesus","0778":"Arinos",
    "0878":"Camanducaia","0879":"Carmópolis de Minas"
}

# Nossa base (Supabase)
conn = psycopg2.connect(host='aws-0-us-west-2.pooler.supabase.com', port=5432, dbname='postgres',
    user='postgres.weaqkaaqalvpbxkxrfee', password='Senha14498620@', connect_timeout=30)
cur = conn.cursor()
cur.execute("SELECT codigo, nome FROM cnj_comarcas WHERE tribunal_codigo = '13'")
nossa = {r[0]: r[1] for r in cur.fetchall()}
cur.close()
conn.close()

print(f"TJMG oficial: {len(oficiais)}")
print(f"Nossa base:   {len(nossa)}")

# Faltam na nossa base
faltam = sorted(set(oficiais.keys()) - set(nossa.keys()))
print(f"\nFALTAM na nossa base ({len(faltam)}):")
for c in faltam:
    print(f"  {c} {oficiais[c]}")

# Extras
extras = sorted(set(nossa.keys()) - set(oficiais.keys()))
print(f"\nEXTRAS na nossa base (sem no TJMG) ({len(extras)}):")
for c in extras:
    print(f"  {c} {nossa[c]}")