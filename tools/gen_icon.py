from PIL import Image, ImageDraw

def gen_icon():
    size = 432  # 4x of 108dp viewport for clean scaling
    bg = (15, 15, 35)   # #0F0F23
    colors = [(255, 87, 51), (51, 255, 87), (51, 87, 255)]  # orange, green, blue

    img = Image.new("RGB", (size, size), bg)
    draw = ImageDraw.Draw(img)

    # circle positions, scaled from 108dp viewport by 4x
    # (cx, cy, radius)  no overlaps, within safe zone (18dp margin)
    circles = [
        (144, 224, 52),   # red   center (36, 56) r=13
        (216, 144, 40),   # green center (54, 36) r=10
        (288, 216, 48),   # blue  center (72, 54) r=12
    ]

    for (cx, cy, r), color in zip(circles, colors):
        bbox = (cx - r, cy - r, cx + r, cy + r)
        draw.ellipse(bbox, fill=color)

    # density targets (size, dir)
    targets = [
        (48, "mdpi"), (72, "hdpi"), (96, "xhdpi"),
        (144, "xxhdpi"), (192, "xxxhdpi"),
    ]

    base_dir = "app/src/main/res"

    for target_size, density in targets:
        resized = img.resize((target_size, target_size), Image.LANCZOS)
        path = f"{base_dir}/mipmap-{density}/ic_launcher.png"
        resized.save(path, "PNG")
        print(f"  {path}  {target_size}x{target_size}")

if __name__ == "__main__":
    gen_icon()
