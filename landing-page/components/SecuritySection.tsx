"use client";

import { motion } from "framer-motion";
import {
  Shield,
  Lock,
  Database,
  EyeOff,
  KeyRound,
  Ban,
} from "lucide-react";

const SECURITY_ITEMS = [
  {
    icon: Shield,
    title: "Row Level Security (RLS)",
    description:
      "Isolamento total de dados no Supabase. Cada usuário acessa apenas o que é seu.",
  },
  {
    icon: KeyRound,
    title: "Auth criptografada",
    description:
      "Autenticação com refresh tokens e criptografia de ponta a ponta nas sessões.",
  },
  {
    icon: Database,
    title: "Armazenamento local seguro",
    description:
      "Rotas e endereços ficam no Room (SQLite) do dispositivo. Nada viaja sem necessidade.",
  },
  {
    icon: EyeOff,
    title: "Zero rastreamento",
    description:
      "Nenhum tracking fora do horário de trabalho. Sua rotina de campo permanece privada.",
  },
  {
    icon: Ban,
    title: "Zero anúncios",
    description:
      "Sem banners, sem trackers de marketing. Foco absoluto na produtividade.",
  },
  {
    icon: Lock,
    title: "Permissões mínimas",
    description:
      "Apenas o essencial: localização e microfone (opcional). Nada mais.",
  },
];

export default function SecuritySection() {
  return (
    <section id="seguranca" className="section-padding relative overflow-hidden">
      {/* Shield glow */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[500px] h-[500px] bg-cyan-electric/5 rounded-full blur-[150px] pointer-events-none" />

      <div className="container-narrow relative">
        <div className="text-center mb-14">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="inline-flex items-center justify-center h-14 w-14 rounded-2xl bg-cyan-electric/10 border border-cyan-electric/30 mb-6"
          >
            <Shield className="h-7 w-7 text-cyan-electric" />
          </motion.div>
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-3"
          >
            Segurança & Privacidade
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl sm:text-4xl font-bold text-white max-w-xl mx-auto"
          >
            Seus dados protegidos.
            <br />
            <span className="text-space-200">Sua confiança reforçada.</span>
          </motion.h2>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
          {SECURITY_ITEMS.map((item, i) => (
            <motion.div
              key={item.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.06 }}
              className="group glass rounded-2xl p-6 border border-white/5 hover:border-cyan-electric/25 transition-all duration-300 hover:shadow-glow"
            >
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-surface-elevated text-cyan-electric mb-4 group-hover:bg-cyan-electric/15 transition-colors">
                <item.icon className="h-5 w-5" />
              </div>
              <h3 className="text-base font-semibold text-white mb-2">
                {item.title}
              </h3>
              <p className="text-sm text-space-200 leading-relaxed">
                {item.description}
              </p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
