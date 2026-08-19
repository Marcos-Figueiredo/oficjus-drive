import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-geist-sans",
  display: "swap",
});

export const metadata: Metadata = {
  title: "OficJus Drive — Rota inteligente para visitas e entregas em campo",
  description:
    "Aplicativo Android nativo 100% offline. Digite por voz, navegue com Waze e confirme tudo com a bolha flutuante exclusiva. Otimize rotas, economize combustível e nunca mais perca um endereço.",
  keywords: [
    "oficjus",
    "rota otimizada",
    "visita em campo",
    "waze",
    "offline",
    "mandados",
    "certidão",
    "android",
  ],
  openGraph: {
    title: "OficJus Drive — Não saia do Waze para registrar uma visita",
    description:
      "Aplicativo Android nativo que transforma a rotina de visitas e entregas em campo em um fluxo inteligente, rápido e 100% offline.",
    type: "website",
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR" className="dark">
      <body className={`${inter.variable} font-sans`}>{children}</body>
    </html>
  );
}
