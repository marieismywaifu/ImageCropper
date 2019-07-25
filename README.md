# ImageCropper
batch cropping of images with the same dimensions
## usage
### inputting the desired resolution
the default resolution is the one of your screen
### opening files
multi file selection is enabled\
_new in v2.0:_ images smaller than the desired resolution are supported
### dragging the cropping window
with the left mouse button\
the offsets ("How many pixels are cut off in this direction?") are displayed in the lower left corner\
_new in v2.0:_ negative offsets ("How many pixels are added in this direction?") are supported
### _new in v2.0:_ selecting the edge mode
- mirror: Default selection. The image is mirrored along the edges. The axis of symmetry goes THROUGH the line of pixels at the edge, not NEXT TO it.
- smear: The pixel at the edge is duplicated. Almost always looks worse than mirror.
- loop: The image is looped.
- transparency: Pixels outside the source image are transparent. See below.
### _new in v2.0:_ switching between day and night mode
Transparent pixels are shown as white, pixels that are not part of the resulting image are shown as black. If it fits your purpose better, you can swap these two colors around.
### saving files
All are cropped the same way. The new resolution is appended to the filename.
