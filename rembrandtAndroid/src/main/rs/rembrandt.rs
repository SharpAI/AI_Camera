#pragma version(1)
#pragma rs java_package_name(ly.img.android.rembrandt)
#pragma rs_fp_relaxed

float allowedColorDistance;
uchar4 colorEquality;
uchar4 colorDiversity;

int *allocationTotalDifferentPixel;
uint32_t* allocationDiffRect;

bool isClean;

int gwidth;
int gheight;

rs_allocation rsAllocationBitmap1;
rs_allocation rsAllocationBitmap2;
rs_allocation rsAllocationComparisonBitmap;

void compareBitmaps(uchar4 *unused, uint32_t x, uint32_t y) {
    uchar4 color1 = rsGetElementAt_uchar4(rsAllocationBitmap1, x, y);
    uchar4 color2 = rsGetElementAt_uchar4(rsAllocationBitmap2, x, y);


    float colorDistance = 0;
    int deltaAlpha = abs(color1.a - color2.a);
    int deltaRed   = abs(color1.r - color2.r);
    int deltaGreen = abs(color1.g - color2.g);
    int deltaBlue  = abs(color1.b - color2.b);
    colorDistance = deltaRed + deltaGreen + deltaBlue;
    //colorDistance += deltaAlpha * deltaAlpha;
    //colorDistance += deltaRed * deltaRed;
    //colorDistance += deltaGreen * deltaGreen;
    //colorDistance += deltaBlue * deltaBlue;
    //colorDistance = sqrt(colorDistance * 255);
    //int deltaAlpha = color1.a - color2.a;
    //int deltaRed   = color1.r - color2.r;
    //int deltaGreen = color1.g - color2.g;
    //int deltaBlue  = color1.b - color2.b;

    //float colorDistance = 0;
    //colorDistance += deltaAlpha * deltaAlpha;
    //colorDistance += deltaRed * deltaRed;
    //colorDistance += deltaGreen * deltaGreen;
    //colorDistance += deltaBlue * deltaBlue;
    //colorDistance = sqrt(colorDistance * 255);

    uchar4 colorToSet;

    if (colorDistance < allowedColorDistance) {
        colorToSet = 0;
    } else {
        colorToSet = 1;
        allocationTotalDifferentPixel[0] ++;
        if(isClean){
            isClean = false;

            // top,bottom - left,right
            allocationDiffRect[0] = y;
            allocationDiffRect[1] = y;
            allocationDiffRect[2] = (gwidth - x);
            allocationDiffRect[3] = (gwidth - x);
            rsDebug("is clean", allocationDiffRect);
        } else {
            allocationDiffRect[0] = min(allocationDiffRect[0],y);
            allocationDiffRect[1] = max(allocationDiffRect[1],y);
            allocationDiffRect[2] = min(allocationDiffRect[2],(gwidth - x));
            allocationDiffRect[3] = max(allocationDiffRect[3],(gwidth - x));
        }
        //rsDebug("total difference pixel is", allocationTotalDifferentPixel[0]);
    }

    rsSetElementAt_uchar4(rsAllocationComparisonBitmap, colorToSet, x, y);
}
