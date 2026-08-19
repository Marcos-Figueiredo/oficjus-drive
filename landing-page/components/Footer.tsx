"use client";

import { MapPin } from "lucide-react";

export default function Footer() {
  return (
    <footer className="border-t border-white/5 bg-surface/30">
      <div className="container-narrow px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-cyan-electric/10 border border-cyan-electric/30">
              <MapPin className="h-4 w-4 text-cyan-electric" strokeWidth={2.5} />
            </div>
            <span className="text-sm font-bold text-white">
              OficJus <span className="text-cyan-electric">Drive</span>
            </span>
          </div>

          <nav className="flex flex-wrap items-center justify-center gap-6 text-sm text-space-300">
            <a href="#recursos" className="hover:text-cyan-electric transition-colors">
              Recursos
            </a>
            <a href="#tecnologia" className="hover:text-cyan-electric transition-colors">
              Tecnologia
            </a>
            <a href="#seguranca" className="hover:text-cyan-electric transition-colors">
              Segurança
            </a>
            <a href="#preco" className="hover:text-cyan-electric transition-colors">
              Preço
            </a>
          </nav>

          <p className="text-xs text-space-400">
            © {new Date().getFullYear()} OficJus Drive. Todos os direitos reservados.
          </p>
        </div>
      </div>
    </footer>
  );
}
