#!/usr/bin/env bash
# Wandelt die Boden-Assets aus Assets/Ground in die Pakete com.dan.ground.<art> um.
# Braucht: python3 mit numpy, pillow  (pip install numpy pillow)
# Aufruf aus dem Projektordner: tools/ground/convert_ground.sh
set -euo pipefail
cd "$(dirname "$0")/../.."
T=$(mktemp -d)
trap 'rm -rf "$T"' EXIT
G=src/com/dan/ground
A=Assets/Ground
P="python3 -I tools/ground/megascans_surface.py"
mkdir -p $G/dirtrough $G/dirtcoarse $G/sandbag $G/corrugated $G/rock

# Megascans-Oberflächen: Farbe (mit AO), Normalen, Höhe, Eigenschaften
$P $A/military_trenches_ground_dirt_rough_02_yd0lacrs_4k.zip $G/dirtrough dirtrough --name "Erde rau"
$P $A/military_trenches_ground_dirt_coarse_01_yd0ldai_4k.zip $G/dirtcoarse dirtcoarse --name "Erde grob"
$P $A/military_trenches_detail_sandbag_canvas_ydzoagbga_4k.zip $G/sandbag sandbag --name "Sandsack-Leinen"
# Wellblech: aus der Höhe entsteht das Profil der Wellen
$P $A/military_trenches_metal_corrugated_02_yd0kejgga_4k.zip $G/corrugated corrugated --name "Wellblech rostig"
# Fels: Netz aus glTF (ohne brauchbare Textur), Gestein aus „Erde rau“, entsättigt
unzip -q -o $A/3d_lowpoly_rock_asset_gltf.zip -d "$T/rock"
python3 -I tools/tree/gltf2dtree.py "$T/rock/scene.gltf" $G/rock/rock_lowpoly.dtree --name "Low-Poly-Fels" --prefix rock
$P $A/military_trenches_ground_dirt_rough_02_yd0lacrs_4k.zip $G/rock rock --name "Gestein (aus Erde rau)" --stone
