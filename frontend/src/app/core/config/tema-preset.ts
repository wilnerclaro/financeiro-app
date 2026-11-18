import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const temaFinanceiro = definePreset(Aura, {
  semantic: {
    primary: {
      50: '{emerald.50}',
      100: '{emerald.100}',
      200: '{emerald.200}',
      300: '{emerald.300}',
      400: '{emerald.400}',
      500: '{emerald.500}',
      600: '{emerald.600}',
      700: '{emerald.700}',
      800: '{emerald.800}',
      900: '{emerald.900}',
      950: '{emerald.950}',
    },
    formField: {
      borderRadius: '{border.radius.lg}',
    },
    content: {
      background: 'light-dark(#ffffff, #141a18)',
      borderColor: 'light-dark(#e6e8e3, #222b28)',
    },
    text: {
      color: 'light-dark(#10231c, #eef3f1)',
      mutedColor: 'light-dark(#5a6560, #93a39c)',
    },
  },
  components: {
    button: {
      root: {
        gap: '6px',
        primary: {
          background: 'var(--acao)',
          hoverBackground: 'color-mix(in srgb, var(--acao) 86%, var(--fundo))',
          activeBackground: 'color-mix(in srgb, var(--acao) 76%, var(--fundo))',
          borderColor: 'var(--acao)',
          hoverBorderColor: 'color-mix(in srgb, var(--acao) 86%, var(--fundo))',
          activeBorderColor: 'color-mix(in srgb, var(--acao) 76%, var(--fundo))',
          color: 'var(--acao-texto)',
          hoverColor: 'var(--acao-texto)',
          activeColor: 'var(--acao-texto)',
          focusRing: {
            color: 'var(--acao)',
            shadow: 'none',
          },
        },
      },
    },
    card: {
      root: {
        shadow: 'none',
      },
    },
    skeleton: {
      root: {
        background: 'light-dark(#e6e9e3, rgba(238, 243, 241, 0.07))',
        animationBackground: 'light-dark(rgba(255, 255, 255, 0.9), rgba(238, 243, 241, 0.1))',
      },
    },
  },
});
