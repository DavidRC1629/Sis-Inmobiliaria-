/**
 * Configuración de marca de la aplicación
 * Puede ser modificado para usar en otros sistemas inmobiliarios
 */
export const APP_BRAND = {
  name: 'AROVI',
  subtitle: 'INMOBILIARIA'
} as const;

export const getBrandDisplayName = (): string => `${APP_BRAND.name} ${APP_BRAND.subtitle}`;
