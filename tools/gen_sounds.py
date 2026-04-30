import wave, struct, math

def gen_plink(filename, start_freq, end_freq, decay, duration=0.12):
    sample_rate = 22050
    num_samples = int(sample_rate * duration)
    sweep = end_freq - start_freq

    with wave.open(filename, "w") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        for i in range(num_samples):
            t = i / sample_rate
            freq = start_freq + sweep * t
            amplitude = math.exp(-t * decay)
            sample = int(math.sin(2 * math.pi * freq * t) * amplitude * 20000)
            sample = max(-32768, min(32767, sample))
            wav.writeframes(struct.pack("<h", sample))

gen_plink("app/src/main/res/raw/plink_wall.wav",   300, 200, 20)
gen_plink("app/src/main/res/raw/plink_circle.wav", 600, 400, 30)
gen_plink("app/src/main/res/raw/boop.wav",         400, 100, 8, duration=0.70)
