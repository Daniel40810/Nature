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
