#!/usr/bin/env bash
# Wandelt alle Baum-Assets aus Assets/Trees in die Pakete com.dan.tree.<art> um.
# Braucht: python3 mit usd-core, scipy, numpy, pillow  (pip install usd-core scipy numpy pillow)
# Aufruf aus dem Projektordner: tools/tree/convert_all.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
P=src/com/dan/tree
PY="python3 -I"
up() { echo "$1" | tr a-z A-Z; }

# Schwarzerle und Greasewood: USD aus dem Procedural Vegetation Editor (mit Ast-Bibliothek und DynamicWind.json)
unzip -q -o Assets/Trees/tree_black_alder_01_usd.zip -d "$T/alder"
unzip -q -o Assets/Trees/shrub_greasewood_01_usd.zip -d "$T/grease"
for v in a b c d; do
  $PY tools/tree/usd2dtree.py "$T/alder/Tree_Black_Alder_01_$(up $v).usd" $P/blackalder/alder_$v.dtree --name "Schwarzerle $(up $v)"
  $PY tools/tree/usd2dtree.py "$T/grease/Shrub_Greasewood_01_$(up $v).usd" $P/greasewood/greasewood_$v.dtree --name "Greasewood $(up $v)"
done

# FBX-Bäume: Stamm und Laubkarten mit Texturen
rm -f $P/{trees60,hptree,mobiletree,supertree}/*_{bark.jpg,leaf.png}
unzip -q -o Assets/Trees/trees_60_fbx.zip -d "$T/t60"
for i in 1 2 3 4 5 6 7 8 9 10; do
  $PY tools/tree/fbx2dtree.py "$T/t60/SM_Trees60_$i.fbx" $P/trees60/trees60_$i.dtree --name "Trees60 $i" \
    --bark "$T/t60/T_Trees60_Trunk.png" --leaf "$T/t60/T_Trees60_Leaf.png" --prefix trees60
done
unzip -q -o Assets/Trees/highpoly_tree_model.zip -d "$T/hp"
H="$T/hp/HighPoly Tree Model"
$PY tools/tree/fbx2dtree.py "$H/Model/SM_HP_Tree.FBX" $P/hptree/hptree.dtree --name "HighPoly-Baum" \
  --bark "$H/Textures/T_HP_Tree_Trunk.PNG" --leaf "$H/Textures/T_HP_Tree_Leaf.PNG" --prefix hptree
unzip -q -o Assets/Trees/mobile_trees_model.zip -d "$T/mobile"
M="$T/mobile/Mobile Trees Model"
$PY tools/tree/fbx2dtree.py "$M/Model/SM_Mobile_Trees.fbx" $P/mobiletree/mobiletree.dtree --name "Mobile-Baum" \
  --bark "$M/Textures/T_Mobile_Trees_Trunk.TGA" --leaf "$M/Textures/T_Mobile_Trees_Leaf.TGA" --prefix mobiletree
unzip -q -o Assets/Trees/super_tree_model.zip -d "$T/super"
U="$T/super/Super Tree Model"
$PY tools/tree/fbx2dtree.py "$U/Model/SM_SuperTree.fbx" $P/supertree/supertree.dtree --name "Super-Baum" \
  --bark "$U/Textures/T_SuperTree_Trunk.png" --leaf "$U/Textures/T_SuperTree_Leaf.png" --prefix supertree

# ---------------------------------------------------------------- Gras (com.dan.grass)
G=src/com/dan/grass
mkdir -p $G/drygrass $G/kikuyu $G/wheatgrass $G/wildgrass $G/vegmix $G/footpath $G/ground
rm -f $G/{drygrass,kikuyu,wheatgrass,wildgrass,vegmix}/*_leaf.png $G/footpath/*_ground.jpg
low() { echo "$1" | tr A-Z a-z; }
# Megascans: je Variante die echten LOD-Stufen (die letzte Stufe ist ein Billboard und bleibt weg),
# Farbe und Deckkraft als getrennte Bilder
megascans() {  # zip ordner-präfix paket präfix varianten stufen
  local zip=$1 stem=$2 pkg=$3 prefix=$4 vars=$5 lods=$6
  mkdir -p "$T/$pkg" && unzip -q -o "$zip" -d "$T/$pkg"
  local base=$(ls "$T/$pkg"/*_BaseColor.jpg | grep -v Billboard | head -1)
  local opac=$(ls "$T/$pkg"/*_Opacity.jpg | grep -v Billboard | head -1)
  for v in $vars; do
    for l in $(seq 0 $((lods - 1))); do
      $PY tools/tree/fbx2dtree.py "$T/$pkg/${stem}_Var${v}_LOD$l.fbx" $G/$pkg/${prefix}_$(low $v)$l.dtree \
        --name "$prefix $v Stufe $l" --all-leaf --leaf "$base" --opacity "$opac" --prefix $prefix
    done
  done
}
megascans Assets/Grass/dry_grass_tbbqejqr_high.zip Dry_Grass_tbbqejqr_High_tbbqejqr drygrass drygrass "A B C D E" 2
megascans Assets/Grass/kikuyu_grass_wiljearqx_mid.zip Kikuyu_Grass_wiljearqx_Mid_wiljearqx kikuyu kikuyu "A B C D E F G H" 3
megascans Assets/Grass/wheat_grass_tdcrdbur_high.zip Wheat_Grass_tdcrdbur_High_tdcrdbur wheatgrass wheatgrass "A B C D E F" 2
megascans Assets/Grass/wild_grass_vlkhcbxia_mid.zip Wild_Grass_vlkhcbxia_Mid_vlkhcbxia wildgrass wildgrass "A B C D E F G H" 3
# Vegetationsmix: Rasenstück aus vielen Meshes mit eigenen Texturen -> Atlas
unzip -q -o Assets/Grass/grass-vegitation-mix.zip -d "$T/vegmix"
$PY tools/tree/fbx2dtree.py "$T/vegmix/source/Grass Vegitation mix.fbx" $G/vegmix/vegmix.dtree --name "Vegetationsmix" \
  --all-leaf --atlas "$T/vegmix/textures" --prefix vegmix
# Fußweg: Gelände aus Fotogrammetrie (glTF)
unzip -q -o Assets/Grass/dusty_foot_path_way_in_grass_garden_gltf.zip -d "$T/footpath"
$PY tools/tree/gltf2dtree.py "$T/footpath/scene.gltf" $G/footpath/footpath.dtree --name "Fußweg im Gras" --prefix footpath --terrain
# Boden für die Gras-Demos: Rasenmaterial Mat 025, nur die Farbe
unzip -q -o Assets/Grass/1024_mat_025_grass.zip -d "$T/mat025"
python3 -I -c "import sys; from PIL import Image; Image.open(sys.argv[1]).convert('RGB').save(sys.argv[2], quality=88)" \
  "$(find "$T/mat025" -name '*basecolor*' | head -1)" $G/ground/mat025_grass.jpg
