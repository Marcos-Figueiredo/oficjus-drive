"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import {
  Check,
  ArrowRight,
  ShieldCheck,
  Headphones,
  Sparkles,
} from "lucide-react";

const BENEFITS = [
  "Rota otimizada Nearest-Neighbor",
  "Bolha flutuante WindowManager",
  "Base CNEFE 3,9M endereços MG",
  "100% offline · 24 MB",
  "Ditado por voz inteligente",
  "Suporte direto e humanizado",
];

export default function PricingCTA() {
  const [email, setEmail] = useState("");
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (email.trim()) {
      setSubmitted(true);
    }
  };

  return (
    <section id="preco" className="section-padding relative overflow-hidden">
      {/* Background glow */}
      <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[600px] h-[300px] bg-cyan-electric/10 rounded-full blur-[120px] pointer-events-none" />

      <div className="container-narrow relative">
        <div className="text-center mb-12">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-3"
          >
            Comece agora
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl sm:text-4xl font-bold text-white max-w-2xl mx-auto"
          >
            Economize até{" "}
            <span className="text-gradient">30% em combustível</span> e tempo
            na sua próxima rota.
          </motion.h2>
        </div>

        {/* Pricing Card */}
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="max-w-lg mx-auto"
        >
          <div className="relative glass-strong rounded-3xl border border-cyan-electric/20 overflow-hidden shadow-glow">
            {/* Badge */}
            <div className="absolute top-0 right-0">
              <div className="bg-cyan-electric text-background text-xs font-bold px-4 py-1.5 rounded-bl-xl">
                7 DIAS GRÁTIS
              </div>
            </div>

            <div className="p-8 sm:p-10">
              <div className="flex items-center gap-2 mb-2">
                <Sparkles className="h-5 w-5 text-lime-neon" />
                <span className="text-sm font-medium text-lime-neon">
                  Plano Único
                </span>
              </div>

              <div className="flex items-baseline gap-1 mb-1">
                <span className="text-5xl font-bold text-white tracking-tight">
                  R$ 29
                </span>
                <span className="text-space-300 text-lg">/mês</span>
              </div>
              <p className="text-sm text-space-300 mb-8">
                Cancele quando quiser. Sem fidelidade.
              </p>

              <ul className="space-y-3 mb-8">
                {BENEFITS.map((b) => (
                  <li key={b} className="flex items-start gap-3">
                    <Check className="h-5 w-5 text-lime-neon flex-shrink-0 mt-0.5" />
                    <span className="text-sm text-space-100">{b}</span>
                  </li>
                ))}
              </ul>

              {/* Form */}
              {!submitted ? (
                <form onSubmit={handleSubmit} className="space-y-3">
                  <label htmlFor="email" className="sr-only">
                    Seu e-mail
                  </label>
                  <input
                    id="email"
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="seu@email.com"
                    className="w-full rounded-xl border border-space-500 bg-surface/60 px-4 py-3.5 text-sm text-white placeholder:text-space-400 focus:outline-none focus:ring-2 focus:ring-cyan-electric focus:border-transparent transition"
                  />
                  <button
                    type="submit"
                    className="w-full btn-primary text-base py-4 animate-pulse-neon group"
                  >
                    Começar Teste Grátis de 7 dias
                    <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                  </button>
                </form>
              ) : (
                <motion.div
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="rounded-xl bg-lime-neon/15 border border-lime-neon/30 p-5 text-center"
                >
                  <Check className="h-8 w-8 text-lime-neon mx-auto mb-2" />
                  <p className="text-white font-semibold">
                    Link enviado para {email}
                  </p>
                  <p className="text-sm text-space-200 mt-1">
                    Verifique sua caixa de entrada.
                  </p>
                </motion.div>
              )}

              {/* Trust badges */}
              <div className="mt-6 flex flex-wrap items-center justify-center gap-4 text-xs text-space-300">
                <span className="flex items-center gap-1.5">
                  <ShieldCheck className="h-3.5 w-3.5 text-cyan-electric" />
                  Dados protegidos
                </span>
                <span className="flex items-center gap-1.5">
                  <Headphones className="h-3.5 w-3.5 text-cyan-electric" />
                  Suporte humanizado
                </span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
