/**
 * Pure Web Audio API waveform synthesizer mimicking Duolingo sound effects
 */
class SoundEngine {
  private ctx: AudioContext | null = null;

  private getContext(): AudioContext {
    if (!this.ctx) {
      const AudioContextClass = window.AudioContext || (window as any).webkitAudioContext;
      this.ctx = new AudioContextClass();
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    return this.ctx;
  }

  private playTone(freq: number, durationSec: number, type: OscillatorType = 'sine', gainVal: number = 0.15): void {
    try {
      const ctx = this.getContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = type;
      osc.frequency.setValueAtTime(freq, ctx.currentTime);

      gain.gain.setValueAtTime(gainVal, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + durationSec);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + durationSec);
    } catch (e) {}
  }

  playComboChime(multiplier: number = 1): void {
    const scale = [523.25, 587.33, 659.25, 783.99, 880.00, 1046.50]; // C5, D5, E5, G5, A5, C6
    const baseIndex = Math.min(scale.length - 1, multiplier - 1);
    const freq = scale[Math.max(0, baseIndex)];

    this.playTone(freq, 0.18, 'triangle', 0.2);
    setTimeout(() => {
      this.playTone(freq * 1.5, 0.12, 'sine', 0.12);
    }, 60);
  }

  playLevelUpFanfare(): void {
    const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
    notes.forEach((freq, idx) => {
      setTimeout(() => {
        this.playTone(freq, 0.28, 'triangle', 0.25);
      }, idx * 100);
    });
  }

  playChestOpen(): void {
    const notes = [440, 554.37, 659.25, 880];
    notes.forEach((freq, idx) => {
      setTimeout(() => {
        this.playTone(freq, 0.22, 'sine', 0.2);
      }, idx * 75);
    });
  }

  playButtonPress(): void {
    this.playTone(320, 0.05, 'sine', 0.08);
  }

  playError(): void {
    this.playTone(180, 0.25, 'sawtooth', 0.15);
  }
}

export const sound = new SoundEngine();
